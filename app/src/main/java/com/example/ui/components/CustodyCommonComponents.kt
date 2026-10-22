package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DisputedCrimson
import com.example.ui.theme.DisputedCrimsonSoft
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.ScheduledSlateBlue
import com.example.ui.theme.ScheduledSlateBlueSoft
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.UserReportedAmber
import com.example.ui.theme.UserReportedAmberSoft
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft

@Composable
fun VerificationBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, label, icon) = when (status.uppercase()) {
        "MUTUALLY_CONFIRMED", "PAID", "ACCEPTED" -> Quad(
            VerifiedEmeraldSoft,
            VerifiedEmerald,
            "Mutually Confirmed",
            Icons.Default.VerifiedUser
        )
        "USER_REPORTED", "UNPAID_REQUESTED" -> Quad(
            UserReportedAmberSoft,
            UserReportedAmber,
            if (status.uppercase() == "UNPAID_REQUESTED") "Payment Requested" else "User-Reported (Unconfirmed)",
            Icons.Default.Info
        )
        "PROPOSED_CHANGE", "PENDING" -> Quad(
            UserReportedAmberSoft,
            UserReportedAmber,
            "Pending Confirmation",
            Icons.Default.Schedule
        )
        "DISPUTED", "DECLINED", "MISSED" -> Quad(
            DisputedCrimsonSoft,
            DisputedCrimson,
            when (status.uppercase()) {
                "DECLINED" -> "Declined"
                "MISSED" -> "Missed Exchange"
                else -> "Disputed Record"
            },
            Icons.Default.ErrorOutline
        )
        else -> Quad(
            ScheduledSlateBlueSoft,
            ScheduledSlateBlue,
            "Scheduled Event",
            Icons.Default.CheckCircle
        )
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        contentColor = fgColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(13.dp),
                tint = fgColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fgColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun CustodyHeroIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B1D3A),
                        Color(0xFF153462),
                        Color(0xFF0D6E6E)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle background circles
            drawCircle(
                color = Color(0xFF2DD4BF).copy(alpha = 0.14f),
                radius = h * 0.65f,
                center = Offset(w * 0.85f, h * 0.25f)
            )
            drawCircle(
                color = Color(0xFF60A5FA).copy(alpha = 0.12f),
                radius = h * 0.5f,
                center = Offset(w * 0.15f, h * 0.85f)
            )

            // Stylized Calendar Card on the right
            val cardWidth = w * 0.26f
            val cardHeight = h * 0.62f
            val cardLeft = w * 0.68f
            val cardTop = h * 0.19f

            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(18f, 18f)
            )
            drawRoundRect(
                color = Color(0xFF2DD4BF).copy(alpha = 0.85f),
                topLeft = Offset(cardLeft + 12f, cardTop + 14f),
                size = Size(cardWidth * 0.42f, 14f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Mini custody blocks inside the graphic
            for (row in 0..2) {
                for (col in 0..3) {
                    val isTeal = (row + col) % 2 == 0
                    drawRoundRect(
                        color = if (isTeal) Color(0xFF2DD4BF).copy(alpha = 0.75f) else Color(0xFF93C5FD).copy(alpha = 0.75f),
                        topLeft = Offset(
                            cardLeft + 12f + col * ((cardWidth - 28f) / 4f),
                            cardTop + 38f + row * 18f
                        ),
                        size = Size(((cardWidth - 36f) / 4f).coerceAtLeast(8f), 11f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Shield outline
            val shieldCenterX = w * 0.58f
            val shieldCenterY = h * 0.52f
            val shieldPath = Path().apply {
                moveTo(shieldCenterX, shieldCenterY - 28f)
                lineTo(shieldCenterX + 24f, shieldCenterY - 18f)
                lineTo(shieldCenterX + 24f, shieldCenterY + 4f)
                quadraticTo(shieldCenterX + 24f, shieldCenterY + 24f, shieldCenterX, shieldCenterY + 32f)
                quadraticTo(shieldCenterX - 24f, shieldCenterY + 24f, shieldCenterX - 24f, shieldCenterY + 4f)
                lineTo(shieldCenterX - 24f, shieldCenterY - 18f)
                close()
            }
            drawPath(
                path = shieldPath,
                color = Color(0xFF0B1D3A).copy(alpha = 0.85f)
            )
            drawPath(
                path = shieldPath,
                color = Color(0xFF2DD4BF),
                style = Stroke(width = 3.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color(0xFF2DD4BF).copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "NEUTRAL • TIMESTAMPED • COURT-READY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF5EEAD4),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Column(modifier = Modifier.fillMaxWidth(0.68f)) {
                Text(
                    text = "Calm, Verifiable Co-Parenting",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Distinguish scheduled events, user-reported logs, and mutually confirmed exchanges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun EmptyRecordsIllustration(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    testTag: String = "empty_state_action",
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                NavyPrimary.copy(alpha = 0.12f),
                                TealSecondary.copy(alpha = 0.18f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.testTag(testTag),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onAction) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
