package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.notifiy.noticeboard.data.model.Purchase
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import com.notifiy.noticeboard.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository(context) }
    var orders by remember { mutableStateOf<List<Purchase>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load orders when screen opens
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                android.util.Log.d("orderHistory", "🔍 === ORDERS SCREEN DEBUG START ===")
                android.util.Log.d("orderHistory", "🔍 Starting to load orders...")
                isLoading = true
                errorMessage = null

                // Step 1: Check user authentication
                android.util.Log.d("orderHistory", "🔍 Step 1: Checking user authentication...")
                val currentUser = repository.getCurrentUser()

                if (currentUser == null) {
                    android.util.Log.w("orderHistory", "❌ No authenticated user found!")
                    android.util.Log.w(
                        "orderHistory",
                        "❌ Firebase Auth current user: ${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid}"
                    )
                    errorMessage = "No user logged in"
                    isLoading = false
                    return@launch
                }

                android.util.Log.d("orderHistory", "✅ User found: ${currentUser.id}")
                android.util.Log.d("orderHistory", "✅ User email: ${currentUser.email}")
                android.util.Log.d("orderHistory", "✅ User name: ${currentUser.name}")

                // Step 2: Query Firebase for purchases
                android.util.Log.d(
                    "orderHistory", "🔍 Step 2: Querying Firebase purchases collection..."
                )
                android.util.Log.d(
                    "orderHistory", "🔍 Querying purchases with userId: '${currentUser.id}'"
                )

                val purchases = repository.getPurchasesByUserId(currentUser.id)

                android.util.Log.d("orderHistory", "📊 Query completed!")
                android.util.Log.d(
                    "orderHistory", "📊 Number of purchases returned: ${purchases.size}"
                )

                if (purchases.isEmpty()) {
                    android.util.Log.w(
                        "orderHistory", "❌ No purchases found for user ${currentUser.id}"
                    )
                    android.util.Log.w("orderHistory", "❌ This could mean:")
                    android.util.Log.w("orderHistory", "   - No purchase records in Firebase")
                    android.util.Log.w("orderHistory", "   - Wrong userId in purchase records")
                    android.util.Log.w(
                        "orderHistory", "   - Firebase security rules blocking access"
                    )
                    android.util.Log.w(
                        "orderHistory", "   - User not the owner of purchase records"
                    )
                } else {
                    android.util.Log.d("orderHistory", "✅ Found ${purchases.size} purchases!")
                    purchases.forEachIndexed { index, purchase ->
                        android.util.Log.d("orderHistory", "📋 Purchase ${index + 1}:")
                        android.util.Log.d("orderHistory", "   - Order ID: ${purchase.orderId}")
                        android.util.Log.d("orderHistory", "   - Plan Name: ${purchase.planName}")
                        android.util.Log.d("orderHistory", "   - Plan ID: ${purchase.planId}")
                        android.util.Log.d("orderHistory", "   - User ID: ${purchase.userId}")
                        android.util.Log.d(
                            "orderHistory", "   - Purchase State: ${purchase.purchaseState}"
                        )
                        android.util.Log.d(
                            "orderHistory", "   - Purchase Date: ${
                                java.text.SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm", java.util.Locale.getDefault()
                                ).format(java.util.Date(purchase.purchaseTime))
                            }"
                        )
                        android.util.Log.d(
                            "orderHistory", "   - Expiry Date: ${
                                java.text.SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm", java.util.Locale.getDefault()
                                ).format(java.util.Date(purchase.expiryTime))
                            }"
                        )
                    }
                }

                orders = purchases
                android.util.Log.d(
                    "orderHistory", "✅ Orders loaded successfully: ${orders.size} items"
                )
                isLoading = false

            } catch (e: Exception) {
                android.util.Log.e("orderHistory", "❌ Error loading orders: ${e.message}")
                android.util.Log.e("orderHistory", "❌ Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e("orderHistory", "❌ Stack trace:", e)
                e.printStackTrace()

                errorMessage = "Error loading orders: ${e.message}"
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading your orders...",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        } else if (errorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Error Loading Orders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "Unknown error occurred",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Retry loading orders
                        scope.launch {
                            try {
                                isLoading = true
                                errorMessage = null

                                val currentUser = repository.getCurrentUser()
                                if (currentUser != null) {
                                    val purchases = repository.getPurchasesByUserId(currentUser.id)
                                    orders = purchases
                                } else {
                                    errorMessage = "No user logged in"
                                }

                                isLoading = false
                            } catch (e: Exception) {
                                errorMessage = "Error loading orders: ${e.message}"
                                isLoading = false
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Retry")
                }
            }
        } else if (orders.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Orders Yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your purchase history will appear here",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, vertical = 35.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            }) {
                            Icon(
                                Icons.Default.ArrowBack, contentDescription = "Back"
                            )
                        }
                        Text(
                            text = "Your Orders",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(end = 30.dp)
                        )
                        Icon(
                            Icons.Outlined.QuestionMark,
                            contentDescription = "Back",
                            modifier = Modifier.size(0.dp)
                        )
                    }
                }

                // Header
//                item {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        IconButton(
//                            onClick = { navController.popBackStack() }
//                        ) {
//                            Icon(
//                                Icons.Default.ArrowBack,
//                                contentDescription = "Back",
//                                tint = MaterialTheme.colorScheme.onBackground
//                            )
//                        }
//
//                        Text(
//                            text = "Your Orders",
//                            fontSize = 20.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onBackground,
//                            modifier = Modifier.weight(1f)
//                        )
//
//                        // Debug button - remove after testing
////                        TextButton(
////                            onClick = {
////                                scope.launch {
////                                    try {
////                                        android.util.Log.d("orderHistory", "🔧 === MANUAL DEBUG BUTTON CLICKED ===")
////                                        android.util.Log.d("orderHistory", "🔧 Current state:")
////                                        android.util.Log.d("orderHistory", "🔧 - isLoading: $isLoading")
////                                        android.util.Log.d("orderHistory", "🔧 - errorMessage: $errorMessage")
////                                        android.util.Log.d("orderHistory", "🔧 - orders.size: ${orders.size}")
////
////                                        android.util.Log.d("orderHistory", "🔧 Getting fresh user data...")
////                                        val currentUser = repository.getCurrentUser()
////                                        android.util.Log.d("orderHistory", "🔧 - Current user: $currentUser")
////                                        android.util.Log.d("orderHistory", "🔧 - User ID: ${currentUser?.id}")
////                                        android.util.Log.d("orderHistory", "🔧 - User email: ${currentUser?.email}")
////
////                                        android.util.Log.d("orderHistory", "🔧 Firebase Auth direct check:")
////                                        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
////                                        android.util.Log.d("orderHistory", "🔧 - Auth user ID: ${authUser?.uid}")
////                                        android.util.Log.d("orderHistory", "🔧 - Auth user email: ${authUser?.email}")
////
////                                        android.util.Log.d("orderHistory", "🔧 Orders in memory:")
////                                        orders.forEachIndexed { index, order ->
////                                            android.util.Log.d("orderHistory", "🔧 Order $index: ${order.planName} (${order.orderId})")
////                                        }
////
////                                        android.util.Log.d("orderHistory", "🔧 Testing direct Firebase query...")
////                                        if (currentUser != null) {
////                                            val freshPurchases = repository.getPurchasesByUserId(currentUser.id)
////                                            android.util.Log.d("orderHistory", "🔧 Fresh query result: ${freshPurchases.size} purchases")
////                                        }
////
////                                        android.util.Log.d("orderHistory", "🔧 === END MANUAL DEBUG ===")
////                                    } catch (e: Exception) {
////                                        android.util.Log.e("orderHistory", "🔧 Debug error: ${e.message}")
////                                        e.printStackTrace()
////                                    }
////                                }
////                            }
////                        ) {
////                            Text("Debug")
////                        }
//                    }
//                }

                // Orders list
                items(orders) { order ->
                    PurchaseCard(
                        purchase = order, onClick = {
                            android.util.Log.d(
                                "orderHistory", "🖱️ Purchase card clicked: ${order.orderId}"
                            )
                            navController.navigate(Screen.PurchaseDetail.createRoute(order.id))
                        })
                }
            }
        }
    }
}

@Composable
fun PurchaseCard(
    purchase: Purchase, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = purchase.planName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadge(purchase.purchaseState)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OrderDetailRow(
                icon = Icons.Default.ShoppingBag, title = "Product ID", value = purchase.planId
            )

            OrderDetailRow(
                icon = Icons.Default.DateRange,
                title = "Period",
                value = purchase.subscriptionPeriod.replaceFirstChar { it.uppercase() })

            OrderDetailRow(
                icon = Icons.Default.Money,
                title = "Price",
                value = "${purchase.currency} ${purchase.price}"
            )

            OrderDetailRow(
                icon = Icons.Default.Schedule,
                title = "Purchase Date",
                value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(
                    Date(
                        purchase.purchaseTime
                    )
                )
            )

            OrderDetailRow(
                icon = Icons.Default.CheckCircle, title = "Valid Until", value = SimpleDateFormat(
                    "dd MMM yyyy", Locale.getDefault()
                ).format(Date(purchase.expiryTime))
            )

            OrderDetailRow(
                icon = Icons.Default.CheckCircle,
                title = "Order ID",
                value = purchase.orderId.take(20) + "..." // Shorten for display
            )
        }
    }
}

@Composable
fun OrderDetailRow(
    icon: ImageVector, title: String, value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$title: ",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusBadge(purchaseState: String) {
    val color = when (purchaseState.lowercase()) {
        "purchased" -> Color(0xFF4CAF50) // Green
        "pending" -> Color(0xFFFFC107) // Amber
        "cancelled", "refunded" -> Color(0xFFF44336) // Red
        else -> Color.Gray
    }
    val text = purchaseState.replaceFirstChar { it.uppercase() }

    Surface(
        color = color, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}