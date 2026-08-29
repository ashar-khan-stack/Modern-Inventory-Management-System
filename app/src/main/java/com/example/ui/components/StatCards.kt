package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.*

fun formatCurrency(amount: Double, symbol: String = "Rs"): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val absAmount = kotlin.math.abs(amount)
    if (absAmount % 1.0 == 0.0) {
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
    } else {
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
    }
    val formatted = formatter.format(absAmount)
    val cleanSymbol = symbol.ifBlank { "Rs" }
    return if (amount < 0) "-$cleanSymbol $formatted" else "$cleanSymbol $formatted"
}

fun formatNumber(number: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(number)
}

@Composable
fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = SuccessGreen,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .testTag("metric_card_${title.lowercase().replace(" ", "_")}")
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        contentColor = badgeColor
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "paid", "in stock", "active" -> Pair(SuccessGreenContainer, Color(0xFF065F46))
        "partial", "partially paid", "low stock", "pending", "on leave" -> Pair(WarningAmberContainer, Color(0xFF92400E))
        "unpaid", "out of stock", "inactive", "cancelled" -> Pair(DangerRedContainer, Color(0xFF991B1B))
        else -> Pair(AppSurfaceVariantLight, AppTextSecondary)
    }

    val displayStatus = when (status.lowercase()) {
        "partial", "partially paid" -> "PARTIALLY PAID"
        "paid" -> "PAID"
        "unpaid" -> "UNPAID"
        else -> status.uppercase()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = displayStatus,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String = "",
    subtitle: String = "",
    message: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actionButton: (@Composable () -> Unit)? = null
) {
    val displayMessage = message.ifEmpty { subtitle }
    val displayTitle = title.ifEmpty { if (displayMessage.isEmpty()) "No Items Found" else "" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            if (displayTitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (displayMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (displayTitle.isNotEmpty()) 4.dp else 16.dp))
                Text(
                    text = displayMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(actionLabel)
                }
            } else if (actionButton != null) {
                Spacer(modifier = Modifier.height(16.dp))
                actionButton()
            }
        }
    }
}
