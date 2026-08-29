package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class ChartBarData(
    val label: String,
    val sales: Double,
    val purchases: Double,
    val expenses: Double
)

data class DonutSliceData(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun ModernRevenueAreaChart(
    title: String,
    subtitle: String,
    dataPoints: List<Pair<String, Double>>,
    lineColor: Color = BrandBluePrimary,
    fillColorStart: Color = BrandBluePrimary.copy(alpha = 0.35f),
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transaction data available yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                val maxValue = (dataPoints.maxOfOrNull { it.second } ?: 100.0).coerceAtLeast(10.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    val width = size.width
                    val height = size.height - 30f // Leave space for labels
                    val spacing = width / (dataPoints.size.coerceAtLeast(2) - 1)

                    val points = dataPoints.mapIndexed { index, pair ->
                        val x = index * spacing
                        val y = height - ((pair.second / maxValue) * height * animatedProgress.value).toFloat()
                        Offset(x, y)
                    }

                    // Draw grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val gridY = height * (i.toFloat() / gridLines)
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, gridY),
                            end = Offset(width, gridY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }

                    // Area fill path
                    if (points.isNotEmpty()) {
                        val areaPath = Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height)
                            close()
                        }

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(fillColorStart, Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Line Stroke
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw Point Circles
                        points.forEach { point ->
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 3.5.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }

                // X-Axis Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dataPoints.forEach { point ->
                        Text(
                            text = point.first,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonBarChart(
    title: String,
    data: List<ChartBarData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Legends
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendItem(label = "Sales", color = BrandBluePrimary)
                    ChartLegendItem(label = "Purchases", color = WarningAmber)
                    ChartLegendItem(label = "Expenses", color = DangerRed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comparison data recorded yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                val maxVal = (data.flatMap { listOf(it.sales, it.purchases, it.expenses) }.maxOrNull() ?: 100.0)
                    .coerceAtLeast(10.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val width = size.width
                    val height = size.height - 24f
                    val groupWidth = width / data.size.coerceAtLeast(1)
                    val barWidth = (groupWidth * 0.22f).coerceAtLeast(6f)

                    data.forEachIndexed { i, item ->
                        val groupStartX = i * groupWidth + (groupWidth - barWidth * 3 - 8f) / 2

                        // Sales bar
                        val salesH = (item.sales / maxVal * height).toFloat()
                        drawRoundRect(
                            color = BrandBluePrimary,
                            topLeft = Offset(groupStartX, height - salesH),
                            size = Size(barWidth, salesH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )

                        // Purchase bar
                        val purchaseH = (item.purchases / maxVal * height).toFloat()
                        drawRoundRect(
                            color = WarningAmber,
                            topLeft = Offset(groupStartX + barWidth + 4f, height - purchaseH),
                            size = Size(barWidth, purchaseH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )

                        // Expense bar
                        val expenseH = (item.expenses / maxVal * height).toFloat()
                        drawRoundRect(
                            color = DangerRed,
                            topLeft = Offset(groupStartX + (barWidth + 4f) * 2, height - expenseH),
                            size = Size(barWidth, expenseH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }

                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    data.forEach {
                        Text(
                            text = it.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutCategoryChart(
    title: String,
    slices: List<DonutSliceData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No category data available",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                val total = slices.sumOf { it.value }.coerceAtLeast(1.0)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 24.dp.toPx()

                            slices.forEach { slice ->
                                val sweepAngle = (slice.value / total * 360f).toFloat()
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 2f, // small gap
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatCurrency(total),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = AppTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        slices.take(5).forEach { slice ->
                            val percent = (slice.value / total * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = slice.label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "$percent%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
