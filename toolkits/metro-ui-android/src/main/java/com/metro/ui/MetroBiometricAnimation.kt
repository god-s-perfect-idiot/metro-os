package com.metro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCirc
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/** Source SVG viewBox (`294×241`); CSS scales the glyph to 0.5. */
private const val BiometricVbWidth = 294f
private const val BiometricVbHeight = 241f
private const val BiometricStrokeWidth = 30f
private const val BiometricPivotX = BiometricVbWidth / 2f
private const val BiometricPivotY = BiometricVbHeight / 2f

/** Default on-screen glyph width — matches the HTML `scale(0.5)` treatment. */
private val BiometricDefaultWidth = (BiometricVbWidth * 0.5f).dp

private const val StartDelayMs = 300L
private const val FadeInMs = 300
private const val SmileMorphMs = 300
private const val SmileTiltMs = 300
private const val SmileSpinMs = 900
private const val EyeMorphMs = 300
private const val WinkMs = 100
private const val HelloFadeMs = 300
private const val HoldMs = 1000L
private const val FadeOutMs = 600
private const val RepeatDelayMs = 300L

private val BiometricGreetingStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 25.sp,
    letterSpacing = 2.sp,
    textAlign = TextAlign.Center,
)

/**
 * Windows Hello–style biometric success animation ([MetroAnimationSuite.Biometric]).
 *
 * Sequence (from the reference HTML/GSAP timeline): fade in → morph smile arc up →
 * tilt −30° → spin to 900° while eyes split → wink → “Hello, {name}!” → fade out.
 *
 * @param name Display name in the greeting (`Hello, {name}!`). Blank → `Hello!`.
 * @param loop When true, repeats after a short delay; when false, plays once then stops.
 * @param playing When false, freezes (does not start or continue the timeline).
 * @param onFinished Invoked after a non-looping cycle completes (post fade-out).
 */
@Composable
fun MetroBiometricAnimation(
    name: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    loop: Boolean = true,
    playing: Boolean = true,
    onFinished: (() -> Unit)? = null,
) {
    val greeting = remember(name) { biometricGreeting(name) }
    val containerAlpha = remember { Animatable(0f) }
    val smileMorph = remember { Animatable(0f) }
    val smileRotation = remember { Animatable(0f) }
    val eyeMorph = remember { Animatable(0f) }
    val winkScaleY = remember { Animatable(1f) }
    val helloAlpha = remember { Animatable(0f) }

    LaunchedEffect(playing, loop, name) {
        if (!playing) return@LaunchedEffect
        do {
            containerAlpha.snapTo(0f)
            smileMorph.snapTo(0f)
            smileRotation.snapTo(0f)
            eyeMorph.snapTo(0f)
            winkScaleY.snapTo(1f)
            helloAlpha.snapTo(0f)

            delay(StartDelayMs)
            containerAlpha.animateTo(1f, tween(FadeInMs))
            smileMorph.animateTo(1f, tween(SmileMorphMs))
            smileRotation.animateTo(
                targetValue = -30f,
                animationSpec = tween(SmileTiltMs, easing = EaseOutCirc),
            )
            coroutineScope {
                launch {
                    smileRotation.animateTo(
                        targetValue = 900f,
                        animationSpec = tween(SmileSpinMs, easing = EaseInOutCirc),
                    )
                }
                launch {
                    delay((SmileSpinMs - EyeMorphMs).toLong())
                    eyeMorph.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(EyeMorphMs, easing = EaseOutQuad),
                    )
                }
            }
            winkScaleY.animateTo(0.25f, tween(WinkMs))
            coroutineScope {
                launch { winkScaleY.animateTo(1f, tween(WinkMs)) }
                launch { helloAlpha.animateTo(1f, tween(HelloFadeMs)) }
            }
            delay(HoldMs)
            containerAlpha.animateTo(0f, tween(FadeOutMs))
            if (loop) delay(RepeatDelayMs)
        } while (loop)
        onFinished?.invoke()
    }

    Column(
        modifier = Modifier
            .width(BiometricDefaultWidth)
            .then(modifier)
            .graphicsLayer { alpha = containerAlpha.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BiometricVbWidth / BiometricVbHeight),
        ) {
            val s = min(size.width / BiometricVbWidth, size.height / BiometricVbHeight)
            val ox = (size.width - BiometricVbWidth * s) / 2f
            val oy = (size.height - BiometricVbHeight * s) / 2f

            withTransform({
                translate(ox, oy)
                scale(s, s)
            }) {
                // Smile arc morphs and spins; eyes are siblings and do not rotate with it.
                rotate(
                    degrees = smileRotation.value,
                    pivot = Offset(BiometricPivotX, BiometricPivotY),
                ) {
                    drawPath(
                        path = smilePath(smileMorph.value),
                        color = color,
                        style = Stroke(
                            width = BiometricStrokeWidth,
                            cap = StrokeCap.Round,
                        ),
                    )
                }

                val t = eyeMorph.value
                val leftCenter = Offset(lerp(148f, 106f, t), lerp(120f, 121f, t))
                val rightCenter = Offset(lerp(148f, 187f, t), lerp(120f, 121f, t))
                val eyeRadius = lerp(53f, 22f, t)

                drawCircle(color = color, radius = eyeRadius, center = leftCenter)

                scale(scaleX = 1f, scaleY = winkScaleY.value, pivot = rightCenter) {
                    drawCircle(color = color, radius = eyeRadius, center = rightCenter)
                }
            }
        }

        BasicText(
            text = greeting,
            style = BiometricGreetingStyle.copy(color = color),
            modifier = Modifier
                .padding(top = 16.dp)
                .graphicsLayer { alpha = helloAlpha.value },
        )
    }
}

/** `Hello, Name!` when [name] is non-blank; otherwise `Hello!`. */
internal fun biometricGreeting(name: String): String {
    val trimmed = name.trim()
    return if (trimmed.isEmpty()) "Hello!" else "Hello, $trimmed!"
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Absolute cubic segments for smile-down (t=0) and smile-up (t=1). */
private fun smilePath(t: Float): Path {
    val m0x = lerp(238.843f, 238.797f, t)
    val m0y = lerp(166f, 75.04f, t)

    val c1x1 = lerp(222.98f, 222.935f, t)
    val c1y1 = lerp(200.268f, 40.772f, t)
    val c1x2 = lerp(188.289f, 188.243f, t)
    val c1y2 = lerp(224.04f, 17f, t)
    val c1x = lerp(148.046f, 148f, t)
    val c1y = lerp(224.04f, 17f, t)

    val c2x1 = lerp(108.426f, 108.38f, t)
    val c2y1 = lerp(224.04f, 17f, t)
    val c2x2 = lerp(74.189f, 74.143f, t)
    val c2y2 = lerp(201f, 40.04f, t)
    val c2x = lerp(58f, 57.954f, t)
    val c2y = lerp(167.587f, 73.453f, t)

    return Path().apply {
        moveTo(m0x, m0y)
        cubicTo(c1x1, c1y1, c1x2, c1y2, c1x, c1y)
        cubicTo(c2x1, c2y1, c2x2, c2y2, c2x, c2y)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroBiometricAnimationDarkPreview() {
    MetroTheme(darkTheme = true) {
        MetroBiometricAnimation(
            name = "Bhakti",
            modifier = Modifier.padding(24.dp),
            loop = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MetroBiometricAnimationLightPreview() {
    MetroTheme(darkTheme = false) {
        MetroBiometricAnimation(
            name = "Bhakti",
            color = Color.Black,
            modifier = Modifier.padding(24.dp),
            loop = true,
        )
    }
}
