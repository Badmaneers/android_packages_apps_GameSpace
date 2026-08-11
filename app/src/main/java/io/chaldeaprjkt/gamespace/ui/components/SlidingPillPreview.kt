package io.chaldeaprjkt.gamespace.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import kotlinx.coroutines.delay

/**
 * Full-width live preview of the sliding-pill notification style.
 *
 * Spans the entire real screen width (not a small box): the pill enters
 * from off-screen on one side, glides continuously across the whole
 * display, exits off-screen on the other side, then loops. Fade style
 * fades in/out in place. Appearance (gradient, material-you, opacity,
 * border, font color) is read straight from AppSettings so the preview
 * matches the real pill.
 */
@Composable
fun SlidingPillPreview(
    animationType: String,
    animationSpeedSeconds: Int,
    showSender: Boolean,
    showMessage: Boolean,
    backgroundOpacityPercent: Int,
    appSettings: AppSettings? = null,
    slideAcross: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val animSpeed = (animationSpeedSeconds * 1000).coerceAtLeast(50)
    val density = LocalDensity.current
    val displayHoldMs = 3000L
    val loopPauseMs = 1200L

    val pillWidthDp = 200.dp
    val pillHeightDp = 40.dp
    val hMarginDp = appSettings?.slidingPillHorizontalMargin?.dp ?: 16.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(84.dp)) {
        val laneWidthPx = with(density) { maxWidth.toPx() }
        val pillWidthPx = with(density) { pillWidthDp.toPx() }
        val hMarginPx = with(density) { hMarginDp.toPx() }

        val isFade = animationType == "fade"
        val leftToRight = animationType.contains("left_right")

        val startX = if (isFade) hMarginPx
            else if (leftToRight) -pillWidthPx
            else laneWidthPx
        val targetX = if (leftToRight) laneWidthPx else -pillWidthPx

        val offsetX = remember { Animatable(startX) }
        val pillAlpha = remember { Animatable(if (isFade) 0f else 1f) }

        LaunchedEffect(
            animationType, animSpeed, slideAcross, showSender,
            showMessage, backgroundOpacityPercent,
        ) {
            while (true) {
                if (isFade) {
                    pillAlpha.snapTo(0f)
                    pillAlpha.animateTo(1f, tween(animSpeed, easing = LinearEasing))
                    delay(displayHoldMs)
                    pillAlpha.animateTo(0f, tween(animSpeed, easing = LinearEasing))
                    delay(loopPauseMs)
                } else {
                    offsetX.snapTo(startX)
                    offsetX.animateTo(targetX, tween(animSpeed, easing = LinearEasing))
                    delay(loopPauseMs)
                }
            }
        }

        val opacityFraction = backgroundOpacityPercent.coerceIn(0, 100) / 100f
        val contentColor = appSettings?.slidingPillFontColor?.let(::Color)
            ?.copy(alpha = opacityFraction) ?: Color.White.copy(alpha = opacityFraction)
        val pillShape = RoundedCornerShape(20.dp)
        val pillShapeModifier = if (appSettings?.slidingPillBorderWidth != null &&
            appSettings.slidingPillBorderWidth > 0
        ) {
            Modifier.border(
                appSettings.slidingPillBorderWidth.dp,
                Color(appSettings.slidingPillBorderColor),
                pillShape,
            )
        } else Modifier

        val fallbackBg = Color.White
        val useGradient = appSettings?.slidingPillUseGradient == true
        val baseColor = when {
            appSettings?.slidingPillUseMaterialYou == true -> Color(0xFFCC1A1A2E)
            useGradient -> null
            else -> appSettings?.slidingPillBackgroundColor?.let(::Color) ?: fallbackBg
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .width(pillWidthDp)
                    .height(pillHeightDp)
                    .graphicsLayer {
                        translationX = offsetX.value
                        alpha = pillAlpha.value
                        shape = pillShape
                        clip = false
                    }
                    .then(
                        if (baseColor != null) {
                            Modifier.background(baseColor.copy(alpha = opacityFraction), pillShape)
                        } else if (appSettings != null && useGradient) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(appSettings.slidingPillGradientStart).copy(alpha = opacityFraction),
                                        Color(appSettings.slidingPillGradientEnd).copy(alpha = opacityFraction),
                                    )
                                ),
                                pillShape,
                            )
                        } else {
                            Modifier
                        }
                    )
                    .then(pillShapeModifier),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.materialsymbols_ic_notifications_rounded_filled),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = contentColor,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = buildString {
                            if (showSender) append("GF")
                            if (showMessage) {
                                if (isNotEmpty()) append(": ")
                                append("AxionOS Best")
                            }
                        },
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}