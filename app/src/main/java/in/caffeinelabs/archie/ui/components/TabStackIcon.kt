package `in`.caffeinelabs.archie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.caffeinelabs.archie.ui.theme.ArchieBlue
import `in`.caffeinelabs.archie.ui.theme.ArchieBorder
import `in`.caffeinelabs.archie.ui.theme.ArchieDarkPill
import `in`.caffeinelabs.archie.ui.theme.ArchieTextPrimary

@Composable
fun TabStackIcon(
    tabCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ArchieTextPrimary
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val cardW = 16.dp.toPx()
            val cardH = 20.dp.toPx()
            val corner = 3.5.dp.toPx()
            val strokeW = 1.5.dp.toPx()

            val left = (size.width - cardW) / 2f
            val top = (size.height - cardH) / 2f
            val cardSize = Size(cardW, cardH)

            if (tabCount > 1) {
                rotate(degrees = 14f, pivot = center) {
                    drawRoundRect(
                        color = ArchieDarkPill,
                        topLeft = Offset(left + 2f, top - 1f),
                        size = cardSize,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                    drawRoundRect(
                        color = ArchieBorder,
                        topLeft = Offset(left + 2f, top - 1f),
                        size = cardSize,
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = strokeW)
                    )
                }
            }

            rotate(degrees = if (tabCount > 1) -10f else 0f, pivot = center) {
                drawRoundRect(
                    color = ArchieDarkPill,
                    topLeft = Offset(left - (if (tabCount > 1) 1f else 0f), top + (if (tabCount > 1) 1f else 0f)),
                    size = cardSize,
                    cornerRadius = CornerRadius(corner, corner)
                )
                drawRoundRect(
                    color = if (tabCount > 1) ArchieBlue else tint,
                    topLeft = Offset(left - (if (tabCount > 1) 1f else 0f), top + (if (tabCount > 1) 1f else 0f)),
                    size = cardSize,
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = strokeW)
                )
            }
        }

        Text(
            text = tabCount.coerceAtLeast(1).toString(),
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
