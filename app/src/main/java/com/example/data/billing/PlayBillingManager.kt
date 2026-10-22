package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BillingUiState(
    val isConnected: Boolean = false,
    val availableProducts: Map<String, ProductDetails> = emptyMap(),
    val statusMessage: String = "Connecting to Google Play Billing...",
    val activeGooglePlaySku: String? = null
)

class PlayBillingManager(
    context: Context,
    private val onEntitlementVerified: (SubscriptionTier) -> Unit
) : PurchasesUpdatedListener {

    private val _billingState = MutableStateFlow(BillingUiState())
    val billingState: StateFlow<BillingUiState> = _billingState.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            querySubscriptionProducts()
            queryExistingPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingState.value = _billingState.value.copy(
                        isConnected = true,
                        statusMessage = "Google Play Billing connected."
                    )
                    querySubscriptionProducts()
                    queryExistingPurchases()
                } else {
                    _billingState.value = _billingState.value.copy(
                        isConnected = false,
                        statusMessage = "Google Play Billing unavailable on this device (${billingResult.debugMessage.ifBlank { "Play Store not signed in or emulator environment" }})."
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = _billingState.value.copy(
                    isConnected = false,
                    statusMessage = "Google Play Billing disconnected."
                )
            }
        })
    }

    private fun querySubscriptionProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionTier.PREMIUM.skuId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionTier.FAMILY_PREMIUM.skuId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = queryProductDetailsResult.productDetailsList.associateBy { it.productId }
                val msg = if (map.isEmpty()) {
                    "Google Play Billing connected. Configure subscription SKUs (${SubscriptionTier.PREMIUM.skuId}, ${SubscriptionTier.FAMILY_PREMIUM.skuId}) in Google Play Console for live checkout."
                } else {
                    "Ready for Google Play subscription checkout."
                }
                _billingState.value = _billingState.value.copy(
                    availableProducts = map,
                    statusMessage = msg
                )
            }
        }
    }

    fun queryExistingPurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
        }
    }

    fun launchSubscriptionFlow(activity: Activity, tier: SubscriptionTier): String? {
        if (!billingClient.isReady) {
            startConnection()
            return "Google Play Billing is not ready on this device. Please ensure Google Play Store is active and signed in."
        }
        val productDetails = _billingState.value.availableProducts[tier.skuId]
            ?: return "Subscription SKU '${tier.skuId}' (${tier.priceLabel}) is not yet published in Google Play Console for this application ID."

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return "No active subscription offer found for '${tier.skuId}'."

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            null
        } else {
            "Play Billing checkout could not start: ${result.debugMessage}"
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _billingState.value = _billingState.value.copy(
                statusMessage = "Purchase canceled by user."
            )
        } else {
            _billingState.value = _billingState.value.copy(
                statusMessage = "Billing update: ${billingResult.debugMessage}"
            )
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val tier = when {
                purchase.products.contains(SubscriptionTier.FAMILY_PREMIUM.skuId) -> SubscriptionTier.FAMILY_PREMIUM
                purchase.products.contains(SubscriptionTier.PREMIUM.skuId) -> SubscriptionTier.PREMIUM
                else -> null
            }
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK && tier != null) {
                        _billingState.value = _billingState.value.copy(
                            activeGooglePlaySku = tier.skuId,
                            statusMessage = "Verified ${tier.label} subscription via Google Play."
                        )
                        onEntitlementVerified(tier)
                    }
                }
            } else if (tier != null) {
                _billingState.value = _billingState.value.copy(
                    activeGooglePlaySku = tier.skuId,
                    statusMessage = "Active ${tier.label} subscription verified."
                )
                onEntitlementVerified(tier)
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
