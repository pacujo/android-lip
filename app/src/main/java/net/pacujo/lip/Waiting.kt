package net.pacujo.lip

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin

// Adapted from: https://stackoverflow.com/a/73968139

@Composable
fun Waiting() {
    val fgColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.primaryContainer
    val thickness = ProgressIndicatorDefaults.CircularStrokeWidth
    val arcDirection by rememberInfiniteTransition()
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1_000,
                    easing = LinearEasing,
                )
            )
        )
    val pen = with(LocalDensity.current) {
        Stroke(width = thickness.toPx(), cap = StrokeCap.Square)
    }

    Canvas(
        modifier = Modifier
            .progressSemantics()
            .size(50.dp)
            .padding(thickness / 2)
    ) {
        drawCircle(
            color = bgColor,
            style = pen,
        )
        drawArc(
            color = fgColor,
            startAngle = arcDirection,
            sweepAngle = 70f,
            useCenter = false,
            style = pen
        )
    }
}