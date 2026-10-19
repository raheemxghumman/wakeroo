package com.wakeroo.app.billing

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
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.wakeroo.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * One-time "Wakeroo Pro" purchase. Entitlement is re-checked with Google Play on every launch
 * and cached, so a paying user never sees Pro vanish after a reinstall or a new phone.
 */
class ProBilling(context: Context, private val settings: SettingsStore, private val scope: CoroutineScope) {
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { handle(purchases.orEmpty()) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() = scope.launch {
        if (!connect()) return@launch
        refreshEntitlement()
        loadPrice()
    }

    suspend fun refreshEntitlement() {
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) handle(result.purchasesList)
    }

    private suspend fun loadPrice() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()),
        ).build()
        val result = client.queryProductDetails(params)
        details = result.productDetailsList?.firstOrNull()
        _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    fun buy(activity: Activity) {
        val product = details
        if (product == null) {
            _message.value = "Purchases aren't available right now. Check your connection and try again."
            start()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()))
            .build()
        client.launchBillingFlow(activity, params)
    }

    fun restore() = scope.launch {
        if (!connect()) {
            _message.value = "Couldn't reach Google Play. Try again in a moment."
            return@launch
        }
        val before = settings.current().pro
        refreshEntitlement()
        val after = settings.current().pro
        _message.value = when {
            after && !before -> "Pro restored. Welcome back!"
            after -> "Pro is already active."
            else -> "No Pro purchase found for this Google account."
        }
    }

    fun clearMessage() { _message.value = null }

    private suspend fun handle(purchases: List<Purchase>) {
        val owned = purchases.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
        }
        // Only ever upgrade from a successful query; never take Pro away on a network error.
        if (owned) settings.update { it.copy(pro = true) }
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    companion object {
        const val PRODUCT_ID = "wakeroo_pro"
    }
}
