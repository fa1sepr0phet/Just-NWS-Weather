package com.nwsweather.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nwsweather.data.local.SavedLocationEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedLocationChips(
    locations: List<SavedLocationEntity>,
    onClick: (SavedLocationEntity) -> Unit,
    onMoveLocation: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    if (locations.isEmpty()) return

    val listState = rememberLazyListState()
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(locations, key = { _, location -> location.id }) { index, location ->
            val isDragging = draggingId == location.id
            val scale by animateFloatAsState(if (isDragging) 1.15f else 1f, label = "drag_scale")
            
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .animateItem()
                    .scale(scale)
                    .offset {
                        if (isDragging) IntOffset(dragOffset.roundToInt(), 0)
                        else IntOffset.Zero
                    }
                    .pointerInput(location.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingId = location.id
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.x
                                
                                val currentItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == location.id }
                                if (currentItemInfo != null) {
                                    val draggedCenter = currentItemInfo.offset + (currentItemInfo.size / 2) + dragOffset
                                    
                                    val targetItem = listState.layoutInfo.visibleItemsInfo.find { item ->
                                        item.key != location.id &&
                                        draggedCenter >= item.offset &&
                                        draggedCenter <= (item.offset + item.size)
                                    }
                                    
                                    if (targetItem != null) {
                                        val fromIndex = locations.indexOfFirst { it.id == location.id }
                                        val toIndex = locations.indexOfFirst { it.id == targetItem.key as Long }
                                        
                                        if (fromIndex != -1 && toIndex != -1) {
                                            val offsetDelta = targetItem.offset - currentItemInfo.offset
                                            onMoveLocation(fromIndex, toIndex)
                                            dragOffset -= offsetDelta
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingId = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffset = 0f
                            }
                        )
                    }
                    .clickable(enabled = draggingId == null) {
                        onClick(location)
                    }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isDragging) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f) 
                            else MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = if (isDragging) 8.dp else 2.dp,
                    shadowElevation = if (isDragging) 4.dp else 0.dp
                ) {
                    Text(
                        text = location.label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
