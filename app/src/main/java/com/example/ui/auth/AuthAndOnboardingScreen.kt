package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.ui.components.CustodyHeroIllustration
import com.example.ui.components.LegalAndDataSafetyDialog
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.LoginPrivacyTermsAndDeletionSection
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TealSecondary

private data class OnboardingFeatureSlide(
    val badge: String,
    val title: String,
    val description: String,
    val bullet1: String,
    val bullet2: String,
    val icon: ImageVector
)

@Composable
fun AuthAndOnboardingScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var slideIndex by remember { mutableIntStateOf(0) }
    var acceptedPrivacyAndTerms by remember { mutableStateOf(true) }
    var activeLegalTab by remember { mutableStateOf<LegalDocumentTab?>(null) }

    val slides = remember {
        listOf(
            OnboardingFeatureSlide(
                badge = "SMART CUSTODY CALENDAR",
                title = "Structured Parenting Schedules for US & UK Families",
                description = "Manage 2-2-3, 2-2-5-5, Alternating Weekends, holidays, and school appointments with an automatic audit history of agreed schedule changes.",
                bullet1 = "Schedule-change requests require co-parent confirmation",
                bullet2 = "Daily, weekly, and monthly views with parenting time ratios",
                icon = Icons.Default.CalendarMonth
            ),
            OnboardingFeatureSlide(
                badge = "TAMPER-EVIDENT DOCUMENTATION",
                title = "Distinguish Allegations from Confirmed Facts",
                description = "Every exchange and incident record is tagged as Scheduled, User-Reported, or Mutually Confirmed—preserving neutral, timestamped records for mediation or legal review.",
                bullet1 = "Automatic lateness calculation from arrival timestamps",
                bullet2 = "Immutable edit & revision history with file hash integrity",
                icon = Icons.Default.VerifiedUser
            ),
            OnboardingFeatureSlide(
                badge = "EXPENSES & CALM CO-PARENT MESSAGING",
                title = "Transparent Reimbursements & Respectful Communication",
                description = "Track shared child expenses in USD ($) or GBP (£), upload receipts, and use the optional BIFF AI writing assistant for child-focused messages.",
                bullet1 = "Custom expense splits (50/50, 60/40, etc.) & PDF/CSV statements",
                bullet2 = "Read receipts & full edit/retraction audit trail",
                icon = Icons.Default.AccountBalanceWallet
            )
        )
    }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavyPrimary,
                        Color(0xFF102A52),
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 0f,
                    endY = 1100f
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "CustodyLog Emblem",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "CustodyLog",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Co-Parenting & Custody Documentation",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF99F6E4)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            CustodyHeroIllustration()
            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Onboarding Feature Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    val activeSlide = slides[slideIndex]
                    AnimatedContent(targetState = activeSlide, label = "onboarding_slide") { slide ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TealSecondary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = slide.icon,
                                        contentDescription = slide.title,
                                        tint = TealSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = slide.badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TealSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = slide.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            FeatureBulletRow(text = slide.bullet1)
                            Spacer(modifier = Modifier.height(6.dp))
                            FeatureBulletRow(text = slide.bullet2)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Slide Indicators & Next/Prev
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            slides.indices.forEach { idx ->
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .width(if (idx == slideIndex) 24.dp else 8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (idx == slideIndex) TealSecondary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { slideIndex = (slideIndex + 1) % slides.size },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("onboarding_next_slide_button")
                        ) {
                            Text(if (slideIndex < slides.size - 1) "Next Feature" else "Review Start")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sign-In & Account Security Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Account Access",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Secure Parent Account Sign-In",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Each parent authenticates with their own Google account. Co-parents only see shared workspace records after you connect via a secure invitation token.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LoginPrivacyTermsAndDeletionSection(
                        acceptedTerms = acceptedPrivacyAndTerms,
                        onAcceptedTermsChange = {
                            acceptedPrivacyAndTerms = it
                            if (it) errorMessage = null
                        },
                        onOpenLegalTab = { tab -> activeLegalTab = tab }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (!acceptedPrivacyAndTerms) {
                                errorMessage = "Please agree to the CustodyLog Privacy Policy and Terms of Service before signing in."
                                return@Button
                            }
                            errorMessage = null
                            isSigningIn = true
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { err ->
                                    isSigningIn = false
                                    errorMessage = err
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying Google Account...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.GppGood,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        val currentLegalTab = activeLegalTab
        if (currentLegalTab != null) {
            LegalAndDataSafetyDialog(
                initialTab = currentLegalTab,
                isAuthenticated = false,
                userEmail = null,
                onDismiss = { activeLegalTab = null }
            )
        }
    }
}

@Composable
private fun FeatureBulletRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(TealSecondary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
