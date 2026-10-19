package com.wakeroo.app.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakeroo.app.ui.theme.Paper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.cos

/**
 * A 3D drum: values sit on a rotating cylinder, tilting away in perspective as they leave the
 * centre. Snaps to a value and ticks a haptic for each one passed.
 */
@Composable
fun DrumWheel(
    values: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
    itemHeight: Dp = 64.dp,
    visibleItems: Int = 5,
    cyclic: Boolean = true,
    label: String = "",
) {
    val haptics = LocalHapticFeedback.current
    val count = values.size
    val total = if (cyclic) count * 400 else count
    val startIndex = if (cyclic) count * 200 + selected else selected
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(state)
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val pad = itemHeight * (visibleItems / 2)
    val latestOnSelected by rememberUpdatedState(onSelected)

    // The item nearest the centre line.
    val centred by remember {
        derivedStateOf {
            val first = state.firstVisibleItemIndex
            val offset = state.firstVisibleItemScrollOffset
            if (offset > itemPx / 2) first + 1 else first
        }
    }

    LaunchedEffect(Unit) {
        var first = true
        snapshotFlow { centred }.distinctUntilChanged().collect { index ->
            if (!first) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            first = false
            latestOnSelected(index % count)
        }
    }

    // External changes (quick chips) move the wheel.
    LaunchedEffect(selected) {
        if (centred % count != selected) {
            val target = if (cyclic) centred - centred % count + selected else selected
            state.animateScrollToItem(target)
        }
    }

    Box(modifier.width(width).height(itemHeight * visibleItems).semantics { contentDescription = "$label ${values[selected]}" }) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = pad),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(total) { index ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            val info = state.layoutInfo
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val itemCentre = item?.let { it.offset + it.size / 2f } ?: viewportCentre
                            val distance = ((itemCentre - viewportCentre) / itemPx).coerceIn(-3f, 3f)
                            val angle = distance * 24f
                            rotationX = -angle
                            cameraDistance = 6f * density
                            alpha = (cos(Math.toRadians(angle.toDouble())).toFloat() - 0.15f).coerceIn(0.08f, 1f)
                            val s = 1f - abs(distance) * 0.08f
                            scaleX = s; scaleY = s
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(values[index % count], style = style, color = Paper, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
