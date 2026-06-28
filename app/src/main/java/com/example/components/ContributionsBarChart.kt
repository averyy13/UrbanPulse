package com.example.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContributionsBarChart(
    data: List<Int>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No activity in the last 30 days.", color = labelColor)
        }
        return
    }

    val maxData = data.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Report Contributions (Last 30 Days)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val numBars = data.size
            val barSpacing = 4.dp.toPx()
            val totalSpacing = barSpacing * (numBars - 1)
            val barWidth = ((canvasWidth - totalSpacing) / numBars).coerceAtLeast(2f) // Ensure minimum width

            data.forEachIndexed { index, value ->
                val barHeight = (value.toFloat() / maxData) * canvasHeight
                val startX = index * (barWidth + barSpacing)
                val startY = canvasHeight - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(startX, startY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
