package com.notifiy.noticeboard.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import com.notifiy.noticeboard.data.iap.IAPManager
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Plan
import com.notifiy.noticeboard.data.model.Purchase as PurchaseModel
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val data: com.notifiy.noticeboard.data.model.User? = null,
    val plans: List<Plan> = emptyList(),
    val selectedPlan: Plan? = null,
    val error: String? = null
)

class SubscriptionViewModel(
    private val context: Context,
    private val repository: FirebaseRepository = FirebaseRepository(context)
) : ViewModel() {
    
    private var purchaseSuccessCallback: (() -> Unit)? = null
    private val recentSubscriptionPeriod = mutableMapOf<String, String>() // productId -> subscriptionPeriod
    private val _lastSuccessfulPurchase = MutableStateFlow<PurchaseModel?>(null)
    val lastSuccessfulPurchase: StateFlow<PurchaseModel?> = _lastSuccessfulPurchase.asStateFlow()
    
    private val iapManager = IAPManager(context)
    
    private val _boardState = MutableStateFlow(SubscriptionUiState())
    val boardState: StateFlow<SubscriptionUiState> = _boardState.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun getErrorMessage(): String? = _errorMessage.value
    
    private val _isIAPReady = MutableStateFlow(false)
    val isIAPReady: StateFlow<Boolean> = _isIAPReady.asStateFlow()
    
    init {
        initializeIAP()
    }
    
    private fun initializeIAP() {
        iapManager.initialize()
        viewModelScope.launch {
            iapManager.isReady.collect { isReady ->
                _isIAPReady.value = isReady
                android.util.Log.d("googleIAP", "SubscriptionViewModel - IAP Ready: $isReady")
                
                if (isReady) {
                    // Query existing purchases when billing is ready
                    viewModelScope.launch {
                        try {
                            val existingPurchases = iapManager.queryPurchases()
                            android.util.Log.d("googleIAP", "SubscriptionViewModel - Found ${existingPurchases.size} existing purchases")
                            existingPurchases.forEach { purchase ->
                                processPurchase(purchase)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("googleIAP", "SubscriptionViewModel - Error querying purchases: ${e.message}")
                        }
                    }
                }
            }
        }
        
        // Observe purchase updates
        viewModelScope.launch {
            iapManager.purchases.collect { purchases ->
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Received ${purchases.size} new purchases")
                purchases.forEach { purchase ->
                    processPurchase(purchase)
                }
            }
        }
    }
    
    fun loadUserDetails() {
        viewModelScope.launch {
            try {
                _boardState.value = _boardState.value.copy(isLoading = true, error = null)
                
                val currentUser = repository.getCurrentUser()
                val plans = repository.getAllPlans()
                
                // Find current user's plan and preselect it
                val currentPlan = currentUser?.let { user ->
                    plans.find { plan ->
                        plan.planName.equals(user.planName, ignoreCase = true) ||
                        plan.planId == user.currentPlanId ||
                        plan.id == user.currentPlanId
                    }
                }
                
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    data = currentUser,
                    plans = plans,
                    selectedPlan = currentPlan ?: plans.firstOrNull(),
                    error = null
                )
                
            } catch (e: Exception) {
                _boardState.value = _boardState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _errorMessage.value = e.message
            }
        }
    }
    
    fun selectPlan(plan: Plan) {
        _boardState.value = _boardState.value.copy(selectedPlan = plan)
    }
    
    fun buyPlanFromGooglePlay(activity: Activity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Starting Google Play billing flow")
                
                // Check if IAP is ready
                if (!_isIAPReady.value) {
                    android.util.Log.d("googleIAP", "SubscriptionViewModel - IAP not ready")
                    _errorMessage.value = "Payment system not ready. Please try again."
                    onResult(false)
                    return@launch
                }
                
                // Use the product IDs from your Play Console screenshot
                val productIds = listOf(
                    "notexp_basic",
                    "notexp_plus", 
                    "notexp_pro",
                    "notexp_unlimited"
                )
                
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Querying Google Play for products: $productIds")
                
                // Query product details from Google Play
                val productDetailsList = iapManager.queryProductDetails(productIds)
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Found ${productDetailsList.size} products")
                
                if (productDetailsList.isEmpty()) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - No product details found")
                    _errorMessage.value = "No subscription plans found. Please check your Play Console setup."
                    onResult(false)
                    return@launch
                }
                
                // Log all available products and their offers
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Found ${productDetailsList.size} products")
                productDetailsList.forEach { productDetails ->
                    android.util.Log.d("googleIAP", "SubscriptionViewModel - Product ID: ${productDetails.productId}, Title: ${productDetails.title}")
                    productDetails.subscriptionOfferDetails?.forEachIndexed { index, offerDetails ->
                        android.util.Log.d("googleIAP", "SubscriptionViewModel -   Offer $index:")
                        android.util.Log.d("googleIAP", "SubscriptionViewModel -     BasePlanId: ${offerDetails.basePlanId}")
                        android.util.Log.d("googleIAP", "SubscriptionViewModel -     OfferId: ${offerDetails.offerId}")
                        android.util.Log.d("googleIAP", "SubscriptionViewModel -     OfferToken: ${offerDetails.offerToken}")
                        offerDetails.pricingPhases.pricingPhaseList.forEachIndexed { pIndex, pricingPhase ->
                            android.util.Log.d("googleIAP", "SubscriptionViewModel -     Pricing Phase $pIndex:")
                            android.util.Log.d("googleIAP", "SubscriptionViewModel -       Formatted Price: ${pricingPhase.formattedPrice}")
                            android.util.Log.d("googleIAP", "SubscriptionViewModel -       Billing Period: ${pricingPhase.billingPeriod}")
                            android.util.Log.d("googleIAP", "SubscriptionViewModel -       Recurrence Mode: ${pricingPhase.recurrenceMode}")
                            android.util.Log.d("googleIAP", "SubscriptionViewModel -       Price Amount Micros: ${pricingPhase.priceAmountMicros}")
                            android.util.Log.d("googleIAP", "SubscriptionViewModel -       Price Currency Code: ${pricingPhase.priceCurrencyCode}")
                        }
                        offerDetails.offerTags.forEachIndexed { tIndex, tag ->
                            android.util.Log.d("googleIAP", "SubscriptionViewModel -     Offer Tag $tIndex: $tag")
                        }
                    } ?: run {
                        android.util.Log.e("googleIAP", "SubscriptionViewModel - No subscription offer details found for product: ${productDetails.productId}")
                    }
                }
                
                // Let Google Play handle the base plan selection
                // Google Play will show all available base plans (monthly/annual) for each product
                // User can choose which base plan they want during the billing flow
                
                // Launch billing flow with the first product and its first offer (monthly)
                // We'll let the user choose the base plan through our UI first
                val productDetails = productDetailsList.first()
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
                
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Using product: ${productDetails.productId} with first offer (monthly)")
                
                if (offerToken.isEmpty()) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - No offer token found for product: ${productDetails.productId}")
                    _errorMessage.value = "No subscription offers available for ${productDetails.productId}"
                    onResult(false)
                    return@launch
                }
                
                // Launch billing flow with first offer (monthly)
                val iapResult = iapManager.launchBillingFlow(activity, productDetails, offerToken)
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Billing flow result: ${iapResult.isSuccess}, error: ${iapResult.errorMessage}")
                
                if (!iapResult.isSuccess) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - Failed to launch billing flow: ${iapResult.errorMessage}")
                    _errorMessage.value = iapResult.errorMessage ?: "Failed to start payment process"
                    onResult(false)
                    return@launch
                }
                
                // The purchase result will be handled in the purchasesUpdatedListener
                onResult(true)
                
            } catch (e: Exception) {
                android.util.Log.e("googleIAP", "SubscriptionViewModel - Exception in buyPlanFromGooglePlay: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
    
    private var isSubscriptionInProgress = false
    
    fun subscribeToPlan(activity: Activity, plan: Plan, isMonthly: Boolean, onResult: (Boolean) -> Unit, onPurchaseSuccess: (() -> Unit)? = null) {
        if (isSubscriptionInProgress) {
            android.util.Log.w("googleIAP", "SubscriptionViewModel - Subscription already in progress, ignoring request")
            onResult(false)
            return
        }
        
        isSubscriptionInProgress = true
        purchaseSuccessCallback = onPurchaseSuccess
        
        viewModelScope.launch {
            try {
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Starting Google Play subscription process for plan: ${plan.planName}, isMonthly: $isMonthly")
                
                // Get current user ID first
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - No authenticated user found")
                    _errorMessage.value = "User not authenticated"
                    onResult(false)
                    return@launch
                }
                
                // Check if this is the free plan
                if (plan.planName.lowercase() == "free") {
                    android.util.Log.d("googleIAP", "SubscriptionViewModel - Handling free plan subscription")
                    subscribeToFreePlan(onResult)
                    return@launch
                }
                
                // Validate plan data
                if (plan.planId.isEmpty()) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - Plan ID is empty for plan: ${plan.planName}")
                    _errorMessage.value = "Invalid plan configuration"
                    onResult(false)
                    return@launch
                }
                
                android.util.Log.d("googleIAP", "SubscriptionViewModel - ===== STARTING SUBSCRIPTION FLOW =====")
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Plan: ${plan.planName}")
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Product ID: '${plan.planId}'")
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Base Plan IDs: ${plan.basePlanId}")
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Amounts: ${plan.amount}")
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Is Monthly: $isMonthly")
                android.util.Log.d("googleIAP", "SubscriptionViewModel - User ID: $userId")
                
                // Validate plan data
                if (plan.planId.isEmpty()) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - ERROR: Plan ID is empty!")
                }
                
                if (plan.basePlanId.isEmpty()) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - WARNING: Base plan IDs are empty!")
                }
                
                android.util.Log.d("googleIAP", "SubscriptionViewModel - About to call IAPManager.launchBillingFlowForSpecificOffer")
                
                // Store the subscription period for this product for later use in processPurchase
                val subscriptionPeriod = if (isMonthly) "monthly" else "annual"
                recentSubscriptionPeriod[plan.planId] = subscriptionPeriod
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Stored subscription period '$subscriptionPeriod' for product '${plan.planId}'")
                
                // Launch Google Play billing-flow with specific offer based on monthly/annual selection
                val billingResult = iapManager.launchBillingFlowForSpecificOffer(
                    activity = activity,
                    productId = plan.planId,
                    isMonthly = isMonthly
                )
                
                if (billingResult.isSuccess) {
                    android.util.Log.d("googleIAP", "SubscriptionViewModel - SUCCESS: Billing flow launched successfully")
                    isSubscriptionInProgress = false
                    onResult(true)
                } else {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - FAILED: Billing flow failed!")
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - Error message: '${billingResult.errorMessage}'")
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - ===== SUBSCRIPTION FLOW ENDED WITH ERROR =====")
                    _errorMessage.value = billingResult.errorMessage ?: "Failed to start purchase"
                    isSubscriptionInProgress = false
                    onResult(false)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("googleIAP", "SubscriptionViewModel - Exception in subscribeToPlan: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = "An error occurred during purchase: ${e.message}"
                isSubscriptionInProgress = false
                onResult(false)
            }
        }
    }
    
    
    fun processPurchase(purchase: Purchase) {
        viewModelScope.launch {
            try {
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Processing purchase: ${purchase.orderId}")
                
                // Get current user
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - No authenticated user found during purchase processing")
                    return@launch
                }
                
                // Check if purchase is already processed
                val existingPurchase = repository.getPurchaseByOrderId(purchase.orderId ?: "")
                if (existingPurchase != null) {
                    android.util.Log.d("googleIAP", "SubscriptionViewModel - Purchase already processed: ${purchase.orderId}")
                    return@launch
                }
                
                // Verify the purchase first
                val isVerified = iapManager.verifyPurchase(purchase)
                if (!isVerified) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - Purchase verification failed: ${purchase.orderId}")
                    _errorMessage.value = "Purchase verification failed"
                    return@launch
                }
                
                // Acknowledge the purchase
                val acknowledged = iapManager.acknowledgePurchase(purchase)
                if (!acknowledged) {
                    android.util.Log.e("googleIAP", "SubscriptionViewModel - Failed to acknowledge purchase: ${purchase.orderId}")
                    _errorMessage.value = "Failed to process purchase"
                    return@launch
                }
                
                // Create purchase record for Firebase
                val productId = purchase.products.firstOrNull() ?: ""
                val planDetails = repository.getPlanById(productId)
                
                // Determine subscription period by checking which base plan offer token was used
                val subscriptionPeriod = determineSubscriptionPeriod(purchase, planDetails)
                val expiryOffsetMs = if (subscriptionPeriod == "monthly") {
                    30L * 24 * 60 * 60 * 1000 // 30 days
                } else {
                    365L * 24 * 60 * 60 * 1000 // 365 days
                }
                
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Determined subscription period: $subscriptionPeriod")
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Expiry offset: ${expiryOffsetMs / (24 * 60 * 60 * 1000)} days")
                
                val purchaseModel = PurchaseModel(
                    userId = userId,
                    orgCode = "", // You can add org code logic here if needed
                    planId = productId,
                    planName = planDetails?.planName ?: getPlanNameFromProductId(productId),
                    subscriptionPeriod = subscriptionPeriod,
                    purchaseToken = purchase.purchaseToken,
                    orderId = purchase.orderId ?: "",
                    purchaseTime = purchase.purchaseTime,
                    expiryTime = purchase.purchaseTime + expiryOffsetMs,
                    purchaseState = "purchased",
                    autoRenewing = purchase.isAutoRenewing,
                    price = "", // You can extract this from product details if needed
                    currency = "", // You can extract this from product details if needed
                    packageName = purchase.packageName,
                    acknowledgementState = "acknowledged"
                )
                
                // Save purchase to Firebase
                val result = repository.createPurchase(purchaseModel)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("googleIAP", "SubscriptionViewModel - Purchase saved to Firebase successfully")
                        
                        // Update user subscription
                        updateUserSubscriptionFromPurchase(purchaseModel)
                    },
                    onFailure = { exception ->
                        android.util.Log.e("googleIAP", "SubscriptionViewModel - Failed to save purchase to Firebase: ${exception.message}")
                        _errorMessage.value = "Failed to save purchase record"
                    }
                )
                
            } catch (e: Exception) {
                android.util.Log.e("googleIAP", "SubscriptionViewModel - Exception in processPurchase: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = e.message
            }
        }
    }
    
    private fun determineSubscriptionPeriod(purchase: Purchase, planDetails: Plan?): String {
        android.util.Log.d("googleIAP", "SubscriptionViewModel - Determining subscription period for purchase: ${purchase.orderId}")
        
        val productId = purchase.products.firstOrNull() ?: ""
        android.util.Log.d("googleIAP", "SubscriptionViewModel - Product ID: $productId")
        
        // First check our recent subscriptions map
        val storedPeriod = recentSubscriptionPeriod[productId]
        if (storedPeriod != null) {
            android.util.Log.d("googleIAP", "SubscriptionViewModel - Found stored subscription period: $storedPeriod")
            // Clean up after use
            recentSubscriptionPeriod.remove(productId)
            return storedPeriod
        }
        
        // Get the plan details from Firebase
        if (planDetails != null) {
            android.util.Log.d("googleIAP", "SubscriptionViewModel - Plan details found: ${planDetails.planName}")
            
            // For free plan, treat as monthly
            if (planDetails.planName.lowercase() == "free") {
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Free plan detected, using monthly")
                return "monthly"
            }
        }
        
        // Fallback: Default to monthly since the bug shows it was defaulting to annual incorrectly
        android.util.Log.w("googleIAP", "SubscriptionViewModel - No stored period found, defaulting to monthly")
        return "monthly"
    }

    private fun extractFreePlanDurationFromBasePlanId(basePlanId: String): Long {
        android.util.Log.d("googleIAP", "SubscriptionViewModel - Extracting duration from basePlanId: $basePlanId")
        
        // Try to extract number from the basePlanId string (e.g., "notexp-free-7days", "free-30-day", etc.)
        val numberRegex = Regex("(\\d+)")
        val match = numberRegex.find(basePlanId)
        
        return if (match != null) {
            val days = match.value.toLongOrNull() ?: 7L
            android.util.Log.d("googleIAP", "SubscriptionViewModel - Extracted duration: $days days")
            days
        } else {
            android.util.Log.w("googleIAP", "SubscriptionViewModel - Could not extract duration from: $basePlanId, defaulting to 7 days")
            7L // Default to 7 days if no number found
        }
    }

    private fun getGooglePlayPlanId(planName: String, isMonthly: Boolean): String {
        return when (planName.lowercase()) {
            "basic" -> if (isMonthly) "notexp_basic_monthly" else "notexp_basic_annual"
            "plus" -> if (isMonthly) "notexp_plus_monthly" else "notexp_plus_annual"
            "pro" -> if (isMonthly) "notexp_pro_monthly" else "notexp_pro_annual"
            "unlimited" -> if (isMonthly) "notexp_unlimited_monthly" else "notexp_unlimited_annual"
            "premium" -> if (isMonthly) "notexp_pro_monthly" else "notexp_pro_annual" // Map premium to pro
            else -> if (isMonthly) "notexp_basic_monthly" else "notexp_basic_annual" // Default to basic
        }
    }
    
    private fun getPlanNameFromProductId(productId: String): String {
        return when {
            productId.contains("unlimited") -> "Unlimited"
            productId.contains("pro") -> "Pro"
            productId.contains("plus") -> "Plus"
            productId.contains("basic") -> "Basic"
            productId.contains("premium") -> "Premium"
            productId.contains("free") -> "Free"
            else -> "Basic"
        }
    }
    
    private fun updateUserSubscriptionFromPurchase(purchase: PurchaseModel) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId.isEmpty()) return@launch
                
                val result = repository.updateUserSubscription(
                    userId = userId,
                    subscriptionExpiry = purchase.expiryTime,
                    currentPlanId = purchase.planId,
                    subscriptionPeriod = purchase.subscriptionPeriod,
                    planName = purchase.planName
                )
                
                result.fold(
                    onSuccess = {
                        android.util.Log.d("googleIAP", "SubscriptionViewModel - User subscription updated from purchase")
                        loadUserDetails() // Reload user details
                        
                        // Store successful purchase for navigation to success screen
                        _lastSuccessfulPurchase.value = purchase
                        
                        // Call success callback for navigation to success screen
                        purchaseSuccessCallback?.invoke()
                        purchaseSuccessCallback = null
                    },
                    onFailure = { exception ->
                        android.util.Log.e("googleIAP", "SubscriptionViewModel - Failed to update user subscription: ${exception.message}")
                        _errorMessage.value = "Purchase successful but failed to update subscription"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("googleIAP", "SubscriptionViewModel - Exception in updateUserSubscriptionFromPurchase: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    
    private suspend fun getCurrentUserId(): String {
        val user = repository.getCurrentUser()
        println("DEBUG: SubscriptionViewModel - getCurrentUserId: ${user?.id}")
        return user?.id ?: ""
    }
    
    fun subscribeToFreePlan(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Get current user ID first
                val userId = getCurrentUserId()
                if (userId.isEmpty()) {
                    _errorMessage.value = "User not authenticated"
                    onResult(false)
                    return@launch
                }
                
                // Get the free plan from repository to extract duration from basePlanId
                val freePlan = repository.getAllPlans().find { it.planName.lowercase() == "free" }
                val freePlanDurationDays = if (freePlan?.basePlanId?.isNotEmpty() == true) {
                    extractFreePlanDurationFromBasePlanId(freePlan.basePlanId[0])
                } else {
                    7L // Default to 7 days if not found
                }
                
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Free plan duration: $freePlanDurationDays days")
                
                // Calculate expiry time: current time + duration in milliseconds
                val currentTime = System.currentTimeMillis()
                val expiryTime = currentTime + (freePlanDurationDays * 24 * 60 * 60 * 1000)
                
                android.util.Log.d("googleIAP", "SubscriptionViewModel - Free plan expiry set to: $expiryTime (${freePlanDurationDays} days from now)")
                
                val result = repository.updateUserSubscription(
                    userId = userId,
                    subscriptionExpiry = expiryTime,
                    currentPlanId = freePlan?.planId ?: "",
                    subscriptionPeriod = "monthly", // Free plans act like monthly subscriptions
                    planName = "Free"
                )
                
                result.fold(
                    onSuccess = {
                        _errorMessage.value = null
                        // Reload user details to reflect the updated subscription
                        loadUserDetails()
                        onResult(true)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message
                onResult(false)
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        iapManager.endConnection()
    }
}

