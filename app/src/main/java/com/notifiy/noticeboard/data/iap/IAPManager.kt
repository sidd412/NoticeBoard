package com.notifiy.noticeboard.data.iap

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.PendingPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class IAPResult(
    val isSuccess: Boolean,
    val purchase: Purchase? = null,
    val errorMessage: String? = null
)

class IAPManager(private val context: Context) {
    
    private var billingClient: BillingClient? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Store the selected subscription period for purchase processing
    private val pendingSubscriptions = mutableMapOf<String, String>() // orderId -> subscriptionPeriod
    
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        android.util.Log.d("googleIAP", "IAPManager - ===== Purchase Complete Event =====")
        android.util.Log.d("googleIAP", "IAPManager - Response code: ${billingResult.responseCode}")
        android.util.Log.d("googleIAP", "IAPManager - Response message: '${billingResult.debugMessage}'")
        android.util.Log.d("googleIAP", "IAPManager - Response code name: ${getResponseCodeName(billingResult.responseCode)}")
        android.util.Log.d("googleIAP", "IAPManager - purchases: ${purchases?.size ?: 0} items")
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null && purchases.isNotEmpty()) {
            android.util.Log.d("googleIAP", "IAPManager - SUCCESS: Processing ${purchases.size} purchases")
            purchases.forEachIndexed { index, purchase ->
                android.util.Log.d("googleIAP", "IAPManager - Purchase $index:")
                android.util.Log.d("googleIAP", "IAPManager -   - OrderId: ${purchase.orderId}")
                android.util.Log.d("googleIAP", "IAPManager -   - State: ${purchase.purchaseState}")
                android.util.Log.d("googleIAP", "IAPManager -   - Products: ${purchase.products}")
                android.util.Log.d("googleIAP", "IAPManager -   - PackageName: ${purchase.packageName}")
            }
            _purchases.value = purchases
            _errorMessage.value = null
        } else {
            android.util.Log.e("googleIAP", "IAPManager - FAILED: Purchase processing failed")
            android.util.Log.e("googleIAP", "IAPManager - Reason: ${getPurchaseFailureReason(billingResult.responseCode)}")
            _errorMessage.value = "Purchase failed: ${billingResult.debugMessage}"
            _purchases.value = emptyList()
        }
    }
    
    fun initialize() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()
            
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                android.util.Log.d("googleIAP", "IAPManager - Billing setup finished: ${billingResult.responseCode}, message: ${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isReady.value = true
                    _errorMessage.value = null
                    android.util.Log.d("googleIAP", "IAPManager - Billing client ready")
                } else {
                    _isReady.value = false
                    _errorMessage.value = "Billing setup failed: ${billingResult.debugMessage}"
                    android.util.Log.e("googleIAP", "IAPManager - Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                _isReady.value = false
                _errorMessage.value = "Billing service disconnected"
                android.util.Log.e("googleIAP", "IAPManager - Billing service disconnected")
            }
        })
    }
    
    suspend fun queryPurchases(): List<Purchase> = suspendCancellableCoroutine { continuation ->
        if (!_isReady.value) {
            continuation.resumeWithException(Exception("Billing client not ready"))
            return@suspendCancellableCoroutine
        }
        
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _purchases.value = purchases
                continuation.resume(purchases)
            } else {
                continuation.resumeWithException(Exception("Query purchases failed: ${billingResult.debugMessage}"))
            }
        }
    }
    
    suspend fun queryProductDetails(productIds: List<String>): List<ProductDetails> = suspendCancellableCoroutine { continuation ->
        if (!_isReady.value) {
            android.util.Log.e("googleIAP", "IAPManager - Billing client not ready!")
            continuation.resumeWithException(Exception("Billing client not ready"))
            return@suspendCancellableCoroutine
        }
        
        // Validate and deduplicate product IDs
        val uniqueProductIds = productIds.distinct()
        android.util.Log.d("googleIAP", "IAPManager - Original product IDs: $productIds")
        android.util.Log.d("googleIAP", "IAPManager - Unique product IDs: $uniqueProductIds")
        
        val productList = uniqueProductIds.map { productId ->
            android.util.Log.d("googleIAP", "IAPManager - Creating product query for: $productId")
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        
        android.util.Log.d("googleIAP", "IAPManager - Constructed ${productList.size} product queries")
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        android.util.Log.d("googleIAP", "IAPManager - Querying product details for: $productIds")
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            android.util.Log.d("googleIAP", "IAPManager - Query product details result: ${billingResult.responseCode}, message: ${billingResult.debugMessage}")
            android.util.Log.d("googleIAP", "IAPManager - Found ${productDetailsList.productDetailsList.size} products")
            
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                android.util.Log.d("googleIAP", "IAPManager - Retrieved products: ${productDetailsList.productDetailsList.map { "${it.productId}" }}")
                continuation.resume(productDetailsList.productDetailsList)
            } else {
                android.util.Log.e("googleIAP", "IAPManager - Query product details failed: ${billingResult.debugMessage}")
                continuation.resumeWithException(Exception("Query product details failed: ${billingResult.debugMessage}"))
            }
        }
    }
    
    suspend fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ): IAPResult = suspendCancellableCoroutine { continuation ->
        if (!_isReady.value) {
            continuation.resume(IAPResult(false, errorMessage = "Billing client not ready"))
            return@suspendCancellableCoroutine
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        android.util.Log.d("googleIAP", "IAPManager - Launch billing flow result: ${billingResult?.responseCode}, message: ${billingResult?.debugMessage}")
        
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            // The purchase flow has started, we'll get the result in purchasesUpdatedListener
            android.util.Log.d("googleIAP", "IAPManager - Billing flow launched successfully")
            continuation.resume(IAPResult(true))
        } else {
            android.util.Log.e("googleIAP", "IAPManager - Failed to launch billing flow: ${billingResult?.debugMessage}")
            continuation.resume(IAPResult(false, errorMessage = "Failed to launch billing flow: ${billingResult?.debugMessage}"))
        }
    }
    
    suspend fun launchBillingFlowWithAllOffers(
        activity: Activity,
        productDetails: ProductDetails
    ): IAPResult = suspendCancellableCoroutine { continuation ->
        if (!_isReady.value) {
            continuation.resume(IAPResult(false, errorMessage = "Billing client not ready"))
            return@suspendCancellableCoroutine
        }
        
        // Create product details params for all offers of this product
        // This should let Google Play show all available base plans
        val productDetailsParamsList = productDetails.subscriptionOfferDetails?.map { offerDetails ->
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerDetails.offerToken)
                .build()
        } ?: emptyList()
        
        if (productDetailsParamsList.isEmpty()) {
            android.util.Log.e("googleIAP", "IAPManager - No offer details found for product: ${productDetails.productId}")
            continuation.resume(IAPResult(false, errorMessage = "No subscription offers found"))
            return@suspendCancellableCoroutine
        }
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        android.util.Log.d("googleIAP", "IAPManager - Launch billing flow with all offers result: ${billingResult?.responseCode}, message: ${billingResult?.debugMessage}")
        
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            // The purchase flow has started, we'll get the result in purchasesUpdatedListener
            android.util.Log.d("googleIAP", "IAPManager - Billing flow with all offers launched successfully")
            continuation.resume(IAPResult(true))
        } else {
            android.util.Log.e("googleIAP", "IAPManager - Failed to launch billing flow with all offers: ${billingResult?.debugMessage}")
            continuation.resume(IAPResult(false, errorMessage = "Failed to launch billing flow: ${billingResult?.debugMessage}"))
        }
    }
    
    suspend fun launchBillingFlowWithMultipleProducts(
        activity: Activity,
        productDetailsList: List<ProductDetails>
    ): IAPResult = suspendCancellableCoroutine { continuation ->
        if (!_isReady.value) {
            continuation.resume(IAPResult(false, errorMessage = "Billing client not ready"))
            return@suspendCancellableCoroutine
        }
        
        // Create product details params for all products with their first offer token
        val productDetailsParamsList = productDetailsList.mapNotNull { productDetails ->
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken != null) {
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            } else {
                android.util.Log.e("googleIAP", "IAPManager - No offer token found for product: ${productDetails.productId}")
                null
            }
        }
        
        if (productDetailsParamsList.isEmpty()) {
            android.util.Log.e("googleIAP", "IAPManager - No valid product details with offer tokens found")
            continuation.resume(IAPResult(false, errorMessage = "No valid subscription offers found"))
            return@suspendCancellableCoroutine
        }
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        android.util.Log.d("googleIAP", "IAPManager - Launch billing flow with multiple products result: ${billingResult?.responseCode}, message: ${billingResult?.debugMessage}")
        
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            // The purchase flow has started, we'll get the result in purchasesUpdatedListener
            android.util.Log.d("googleIAP", "IAPManager - Billing flow with multiple products launched successfully")
            continuation.resume(IAPResult(true))
        } else {
            android.util.Log.e("googleIAP", "IAPManager - Failed to launch billing flow with multiple products: ${billingResult?.debugMessage}")
            continuation.resume(IAPResult(false, errorMessage = "Failed to launch billing flow: ${billingResult?.debugMessage}"))
        }
    }
    
    suspend fun launchBillingFlowForSpecificBasePlan(
        activity: Activity,
        productId: String,
        basePlanId: String
    ): IAPResult = suspendCancellableCoroutine { continuation ->
        if (!_isReady.value) {
            continuation.resume(IAPResult(false, errorMessage = "Billing client not ready"))
            return@suspendCancellableCoroutine
        }

        // Use a separate coroutine scope for the nested suspend function call
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // First query product details
                val productDetailsList = queryProductDetails(listOf(productId))
                
                val productDetails = productDetailsList.firstOrNull()
                if (productDetails == null) {
                    continuation.resume(IAPResult(false, errorMessage = "Product not found: $productId"))
                    return@launch
                }

                // Find the specific base plan offer token
                val targetOffer = productDetails.subscriptionOfferDetails?.find { offer ->
                    val offerBasePlanId = offer.basePlanId ?: ""
                    android.util.Log.d("googleIAP", "IAPManager - Comparing base plan IDs: offer='$offerBasePlanId' vs target='$basePlanId'")
                    offerBasePlanId == basePlanId || offerBasePlanId.contains(basePlanId)
                }

                if (targetOffer == null) {
                    android.util.Log.e("googleIAP", "IAPManager - Base plan not found: $basePlanId in offers: ${productDetails.subscriptionOfferDetails?.map { it.basePlanId }}")
                    continuation.resume(IAPResult(false, errorMessage = "Base plan not found: $basePlanId"))
                    return@launch
                }

                // Launch billing flow with specific base plan
                val billingResult = launchBillingFlow(activity, productDetails, targetOffer.offerToken)
                continuation.resume(billingResult)
                
            } catch (e: Exception) {
                android.util.Log.e("googleIAP", "IAPManager - Error in launchBillingFlowForSpecificBasePlan: ${e.message}")
                continuation.resume(IAPResult(false, errorMessage = "Error querying product details: ${e.message}"))
            }
        }
    }

    private fun extractBasePlanId(basePlanId: String): String {
        // Remove the "-monthly" or "-yearly" suffix to get the base plan ID
        return basePlanId.replace("-monthly", "").replace("-yearly", "")
    }

    suspend fun launchBillingFlowForSpecificOffer(
        activity: Activity,
        productId: String,
        isMonthly: Boolean
    ): IAPResult = suspendCancellableCoroutine { continuation ->
        if (!_isReady.value) {
            continuation.resume(IAPResult(false, errorMessage = "Billing client not ready"))
            return@suspendCancellableCoroutine
        }

        // Use a separate coroutine scope for the nested suspend function call
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // First query product details
                val productDetailsList = queryProductDetails(listOf(productId))
                
                val productDetails = productDetailsList.firstOrNull()
                if (productDetails == null) {
                    continuation.resume(IAPResult(false, errorMessage = "Product not found: $productId"))
                    return@launch
                }

                android.util.Log.d("googleIAP", "IAPManager - Found product: ${productDetails.productId}")
                android.util.Log.d("googleIAP", "IAPManager - Available offers: ${productDetails.subscriptionOfferDetails?.map { "${it.basePlanId} (${it.offerToken.take(10)}...)" }}")
                android.util.Log.d("googleIAP", "IAPManager - Looking for ${if (isMonthly) "monthly" else "yearly"} offer")

                // Find the specific offer based on isMonthly preference
                val targetOffer = productDetails.subscriptionOfferDetails?.find { offer ->
                    val basePlanId = offer.basePlanId ?: ""
                    android.util.Log.d("googleIAP", "IAPManager - Checking offer: $basePlanId")
                    if (isMonthly) {
                        basePlanId.contains("monthly") || basePlanId.contains("montly")
                    } else {
                        basePlanId.contains("yearly") || basePlanId.contains("annual")
                    }
                }

                if (targetOffer == null) {
                    android.util.Log.e("googleIAP", "IAPManager - No ${if (isMonthly) "monthly" else "yearly"} offer found")
                    continuation.resume(IAPResult(false, errorMessage = "No ${if (isMonthly) "monthly" else "yearly"} offer found"))
                    return@launch
                }

                android.util.Log.d("googleIAP", "IAPManager - Selected offer: ${targetOffer.basePlanId} (${targetOffer.offerToken.take(10)}...)")

                // Launch billing flow with specific offer only
                val billingResult = launchBillingFlowWithSingleOffer(activity, productDetails, targetOffer)
                continuation.resume(billingResult)
                
            } catch (e: Exception) {
                android.util.Log.e("googleIAP", "IAPManager - Error in launchBillingFlowWithAllOffers: ${e.message}")
                continuation.resume(IAPResult(false, errorMessage = "Error querying product details: ${e.message}"))
            }
        }
    }

    private suspend fun launchBillingFlowWithSingleOffer(
        activity: Activity,
        productDetails: ProductDetails,
        selectedOffer: ProductDetails.SubscriptionOfferDetails
    ): IAPResult = suspendCancellableCoroutine { continuation ->
        
        android.util.Log.d("googleIAP", "IAPManager - Creating billing flow params for single offer")
        android.util.Log.d("googleIAP", "IAPManager - Product: ${productDetails.productId}, Offer: ${selectedOffer.basePlanId}")
        android.util.Log.d("googleIAP", "IAPManager - Offer token: ${selectedOffer.offerToken}")
        android.util.Log.d("googleIAP", "IAPManager - Current user: ${getCurrentUserEmail()}")
        
        // Create product details params for single offer only
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(selectedOffer.offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams)) // Only ONE offer
            .build()
            
        android.util.Log.d("googleIAP", "IAPManager - About to launch Google Play billing flow")
        android.util.Log.d("googleIAP", "IAPManager - Activity: ${activity.javaClass.simpleName}")
        android.util.Log.d("googleIAP", "IAPManager - Activity package: ${activity.packageName}")

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        android.util.Log.d("googleIAP", "IAPManager - Launch billing flow with single offer result: ${billingResult?.responseCode}, message: ${billingResult?.debugMessage}")
        
        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            android.util.Log.d("googleIAP", "IAPManager - Billing flow launched successfully for ${selectedOffer.basePlanId}")
            continuation.resume(IAPResult(true))
        } else {
            android.util.Log.e("googleIAP", "IAPManager - Failed to launch billing flow: ${billingResult?.debugMessage}")
            continuation.resume(IAPResult(false, errorMessage = "Failed to launch billing flow: ${billingResult?.debugMessage}"))
        }
    }

    private fun getResponseCodeName(responseCode: Int): String {
        return when (responseCode) {
            BillingClient.BillingResponseCode.OK -> "OK"
            BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
            BillingClient.BillingResponseCode.ERROR -> "ERROR"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
            else -> "UNKNOWN($responseCode)"
        }
    }
    
    private fun getPurchaseFailureReason(responseCode: Int): String {
        return when (responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> "User canceled the purchase"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Google Play service is unavailable"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing is unavailable on this device"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Item is unavailable for purchase"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Developer error in purchase flow"
            BillingClient.BillingResponseCode.ERROR -> "Generic error occurred"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Item already owned by user"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "Item not owned (for consumptions)"
            else -> "Unknown error ($responseCode)"
        }
    }
    
    private fun getCurrentUserEmail(): String {
        try {
            return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "No user logged in"
        } catch (e: Exception) {
            return "Authentication error: ${e.message}"
        }
    }

    suspend fun verifyPurchase(purchase: Purchase): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Basic validation checks
            val isValid = purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.purchaseToken.isNotEmpty() &&
                    !purchase.orderId.isNullOrEmpty() &&
                    purchase.products.isNotEmpty()

            if (!isValid) {
                android.util.Log.e("googleIAP", "IAPManager - Purchase validation failed: state=${purchase.purchaseState}, token=${purchase.purchaseToken.take(10)}..., orderId=${purchase.orderId}")
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            // Check if purchase is acknowledged
            val isAcknowledged = if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                purchase.isAcknowledged
            } else {
                true
            }

            android.util.Log.d("googleIAP", "IAPManager - Purchase verification passed: orderId=${purchase.orderId}, acknowledged=$isAcknowledged")
            continuation.resume(isValid)
        } catch (e: Exception) {
            android.util.Log.e("googleIAP", "IAPManager - Purchase verification error: ${e.message}")
            continuation.resume(false)
        }
    }

    suspend fun acknowledgePurchase(purchase: Purchase): Boolean = suspendCancellableCoroutine { continuation ->
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            
            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        } else {
            continuation.resume(true) // Already acknowledged or not purchased
        }
    }
    
    fun endConnection() {
        billingClient?.endConnection()
        _isReady.value = false
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}