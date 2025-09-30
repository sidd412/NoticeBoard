package com.notifiy.noticeboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Plan
import com.notifiy.noticeboard.ui.viewmodel.SubscriptionViewModel
import com.notifiy.noticeboard.ui.viewmodel.cachedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navController: NavController,
    subscriptionViewModel: SubscriptionViewModel = cachedViewModel(SubscriptionViewModel::class.java)
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isMonthly by remember { mutableStateOf(true) }

    val boardState by subscriptionViewModel.boardState.collectAsState()

    // Load user details and plans
    LaunchedEffect(Unit) {
        subscriptionViewModel.loadUserDetails()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 45.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Choose A Plan",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = {}) {}
                }
            }
            // User info
            item {
                boardState.data?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "User Subscription",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Manage your subscription plan",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Monthly/Annually Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(.3f),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .padding(vertical = 5.dp, horizontal = 6.dp), horizontalArrangement = Arrangement.Center
                    ) {
                        // Monthly Button
                        Button(
                            onClick = { isMonthly = true },
                            modifier = Modifier.height(35.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMonthly) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(35.dp)
                        ) {
                            Text(
                                text = "Monthly",
                                color = if (isMonthly) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Annually Button
                        Button(
                            onClick = { isMonthly = false },
                            modifier = Modifier.height(35.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isMonthly) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(35.dp)
                        ) {
                            Text(
                                text = "Annually",
                                color = if (!isMonthly) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Plans LazyRow
            item {
                val filteredPlans = if (isMonthly) {
                    boardState.plans
                } else {
                    boardState.plans.filter { it.planName.lowercase() != "free" }
                }

                // Sort plans by amount (free first, then by price)
                val sortedPlans = filteredPlans.sortedWith(compareBy<Plan> { plan ->
                    when {
                        plan.planName.lowercase() == "free" -> 0.0
                        isMonthly -> {
                            // Extract numeric value from amount string for monthly
                            val amountStr = plan.amount.replace(Regex("[^0-9.]"), "")
                            amountStr.toDoubleOrNull() ?: Double.MAX_VALUE
                        }

                        else -> {
                            // Extract numeric value from annualPrice string for annual
                            val amountStr = plan.annualPrice.replace(Regex("[^0-9.]"), "")
                            amountStr.toDoubleOrNull() ?: Double.MAX_VALUE
                        }
                    }
                })

                if (sortedPlans.isNotEmpty()) {
                    PlansLazyRow(
                        plans = sortedPlans,
                        selectedPlan = boardState.selectedPlan,
                        isMonthly = isMonthly,
                        onPlanSelected = { plan -> subscriptionViewModel.selectPlan(plan) })
                }
            }

            // Selected Plan Details
            item {
                boardState.selectedPlan?.let { plan ->
                    PlanDetailsCard(plan = plan)
                }
            }
        }

        // Subscribe Button
        boardState.selectedPlan?.let { plan ->
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 25.dp)
            ) {
                 Button(
                     onClick = {
                         isLoading = true
                         subscriptionViewModel.subscribeToPlan(plan, isMonthly) { success ->
                             isLoading = false
                             if (success) {
                                 navController.popBackStack()
                             } else {
                                 errorMessage = "Failed to subscribe. Please try again."
                             }
                         }
                     },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Subscribe Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Error message
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PlansLazyRow(
    plans: List<Plan>, selectedPlan: Plan?, isMonthly: Boolean, onPlanSelected: (Plan) -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to selected plan
    LaunchedEffect(selectedPlan) {
        selectedPlan?.let { plan ->
            val selectedIndex = plans.indexOfFirst { it.id == plan.id }
            if (selectedIndex != -1) {
                listState.animateScrollToItem(selectedIndex)
            }
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(plans) { plan ->
            val isSelected = selectedPlan?.id == plan.id

            PlanCard(
                plan = plan,
                isSelected = isSelected,
                isMonthly = isMonthly,
                onClick = { onPlanSelected(plan) },
                modifier = Modifier
                    .height(if (isSelected) 125.dp else 120.dp)
                    .width(100.dp)
                    .alpha(if (isSelected) 1f else 0.7f)
            )
        }
    }
}

@Composable
fun PlanCard(
    plan: Plan,
    isSelected: Boolean,
    isMonthly: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier
        .clickable { onClick() }
        .border(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(12.dp)
        ), colors = CardDefaults.cardColors(
        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    ), shape = RoundedCornerShape(12.dp)) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Plan Icon
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Plan Name
                Text(
                    text = plan.planName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )

                // Price
                Text(
                    text = if (isMonthly) plan.amount else plan.annualPrice,
                    fontSize = 10.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Selection Checkmark
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PlanDetailsCard(plan: Plan) {
    Card(
        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Benefits included in ${plan.planName} plan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features based on plan fields
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (plan.pages > 0) {
                    SubscriptionFeature(
                        text = "Create up to ${plan.pages} pages", isIncluded = true
                    )
                }

                if (plan.images > 0) {
                    SubscriptionFeature(
                        text = "Upload up to ${plan.images} images", isIncluded = true
                    )
                }

                if (plan.pdfs > 0) {
                    SubscriptionFeature(
                        text = "Upload up to ${plan.pdfs} PDF files", isIncluded = true
                    )
                }

                if (plan.links > 0) {
                    SubscriptionFeature(
                        text = "Add up to ${plan.links} links", isIncluded = true
                    )
                }

                if (plan.invitePeople > 0) {
                    SubscriptionFeature(
                        text = "Invite up to ${plan.invitePeople} people to collaborate",
                        isIncluded = true
                    )
                }

                if (plan.boardCreation) {
                    SubscriptionFeature(
                        text = "Create multiple notice boards", isIncluded = true
                    )
                }

                if (plan.boardMonetization) {
                    SubscriptionFeature(
                        text = "Monetize your notice boards", isIncluded = true
                    )
                }

                if (plan.realTimeNotifictaion) {
                    SubscriptionFeature(
                        text = "Real-time notifications for updates", isIncluded = true
                    )
                }

                if (plan.queryByUser) {
                    SubscriptionFeature(
                        text = "Allow subscribers to send queries", isIncluded = true
                    )
                }

                if (plan.queryForAnalyticsReport) {
                    SubscriptionFeature(
                        text = "Advanced analytics and reports", isIncluded = true
                    )
                }

                if (plan.aiNoteFeature) {
                    SubscriptionFeature(
                        text = "AI-powered note generation", isIncluded = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionFeature(
    text: String, isIncluded: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isIncluded) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isIncluded) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun isSubscriptionActive(user: com.notifiy.noticeboard.data.model.User): Boolean {
    val currentTime = System.currentTimeMillis()
    return user.currentPlanId.isNotEmpty() && user.subscriptionExpiry > currentTime
}