// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashchip.flashgallery.model.GalleryItem
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastScrollBar(
    gridState: LazyGridState,
    items: List<GalleryItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var barHeightPx by remember { mutableFloatStateOf(1f) }

    // Find current active month title based on first visible item
    val activeMonthTitle by remember(items) {
        derivedStateOf {
            val visibleIdx = gridState.firstVisibleItemIndex
            if (visibleIdx in items.indices) {
                // Find nearest preceding month header
                for (i in visibleIdx downTo 0) {
                    val item = items[i]
                    if (item is GalleryItem.MonthHeader) {
                        return@derivedStateOf item.title
                    }
                }
            }
            ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .onGloballyPositioned { coordinates ->
                barHeightPx = coordinates.size.height.toFloat()
            },
        contentAlignment = Alignment.TopEnd
    ) {
        // Floating Month Bubble shown while dragging or scrolling
        val showBubble = isDragging || gridState.isScrollInProgress
        AnimatedVisibility(
            visible = showBubble && activeMonthTitle.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = -70.dp.roundToPx(),
                        y = dragOffsetY.coerceIn(20f, (barHeightPx - 100f).coerceAtLeast(20f)).roundToInt()
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = activeMonthTitle,
                    color = FlashCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Fast Scroll Thumb Pill
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .offset {
                    val thumbY = if (isDragging) {
                        dragOffsetY.coerceIn(0f, (barHeightPx - 60f).coerceAtLeast(0f))
                    } else {
                        val total = items.size.coerceAtLeast(1)
                        val fraction = (gridState.firstVisibleItemIndex.toFloat() / total).coerceIn(0f, 1f)
                        fraction * (barHeightPx - 60f).coerceAtLeast(0f)
                    }
                    IntOffset(0, thumbY.roundToInt())
                }
                .width(6.dp)
                .height(48.dp)
                .clip(CircleShape)
                .background(if (isDragging) FlashCyan else FlashCyan.copy(alpha = 0.5f))
                .pointerInput(items) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragOffsetY = offset.y
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount).coerceIn(0f, barHeightPx)
                            val fraction = (dragOffsetY / barHeightPx).coerceIn(0f, 1f)
                            val targetIndex = (fraction * items.lastIndex).roundToInt()
                            coroutineScope.launch {
                                gridState.scrollToItem(targetIndex)
                            }
                        }
                    )
                }
        )
    }
}
