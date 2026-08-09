package io.chaldeaprjkt.gamespace.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.chaldeaprjkt.gamespace.R

/**
 * Live preview of the sliding-pill notification style.
 *
 * Shared between the standalone Settings screen and the in-game overlay's
 * long-press customisation popup, so both surfaces render an identical,
 * always-current preview of whatever is currently configured.
 *
 * Replays the slide/fade-in animation any time one of the customisation
 * parameters changes, then holds ("stays stale") at its resting position.
 */
@Composable
fun SlidingPillPreview(
    animationType: String,
    animationSpeedSeconds: Int,
    capsuleMode: Boolean,
    showIcon: Boolean,
    showSender: Boolean,
    showMessage: Boolean,
    fontSizeSp: Int,
    backgroundOpacityPercent: Int,
    shadowStrengthPercent: Int,
    slideAcross: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val animSpeed = (animationSpeedSeconds * 1000).coerceAtLeast(50)
    val density = LocalDensity.current

    val previewWidthDp = 280.dp
    val pillWidthDp = 200.dp
    val pillHeightDp = 40.dp

    val isFade = animationType == "fade"
    val startX = with(density) {
        when {
            isFade -> 0f
            animationType.contains("right_left") -> previewWidthDp.toPx()
            else -> -pillWidthDp.toPx()
        }
    }
    val endX = with(density) { (previewWidthDp - pillWidthDp).toPx() }

    val offsetX = remember { Animatable(startX) }
    val pillAlpha = remember { Animatable(if (isFade) 0f else 1f) }

    // Replay the slide/fade-in any time a relevant setting changes, then hold.
    LaunchedEffect(
        animationType, animSpeed, capsuleMode, showIcon, showSender,
        showMessage, fontSizeSp, backgroundOpacityPercent, shadowStrengthPercent,
    ) {
        offsetX.snapTo(startX)
        pillAlpha.snapTo(if (isFade) 0f else 1f)
        if (isFade) {
            pillAlpha.animateTo(1f, tween(animSpeed, easing = LinearEasing))
        } else {
            offsetX.animateTo(endX, tween(animSpeed, easing = LinearEasing))
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(previewWidthDp)
                .height(80.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .width(pillWidthDp)
                    .height(pillHeightDp)
                    .graphicsLayer {
                        translationX = offsetX.value
                        alpha = pillAlpha.value
                        shadowElevation = shadowStrengthPercent / 100f * 16f * density.density
                        shape = if (capsuleMode) RoundedCornerShape(20.dp) else RoundedCornerShape(8.dp)
                        clip = false
                    }
                    .background(
                        // Black over the navy backdrop, unlike the previous same-hue
                        // backdrop color, so the pill stays visible at every opacity.
                        Color.Black.copy(alpha = backgroundOpacityPercent.coerceIn(0, 100) / 100f),
                        if (capsuleMode) RoundedCornerShape(20.dp) else RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showIcon) {
                        Icon(
                            painter = painterResource(R.drawable.materialsymbols_ic_notifications_rounded_filled),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = buildString {
                            if (showSender) append("GF")
                            if (showMessage) {
                                if (isNotEmpty()) append(": ")
                                append("I Love You")
                            }
                        },
                        fontSize = fontSizeSp.sp,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
