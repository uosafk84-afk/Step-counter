package com.stepcounter.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft warm gradient with a few colour glows. No real blur, so it can't glitch. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Pal.BgTop, Pal.BgBottom)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Pal.Coral.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.04f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Pal.Mint.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.62f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Pal.Amber.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.98f),
                        radius = size.width * 0.8f
                    )
                )
            },
        content = content
    )
}

/** Frosted-glass surface: translucent white gradient plus a crystal edge. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val base = modifier
        .clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.78f), tint.copy(alpha = 0.48f))))
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color.White, Color(0x228A9AA5))),
            shape
        )
    val m = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(m, content = content)
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val anim by animateFloatAsState(
        progress.coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing), label = "ring"
    )
    val colors = if (progress >= 1f) listOf(Pal.Mint, Color(0xFF8FE3C2), Pal.Mint)
    else listOf(Pal.Coral, Pal.Amber, Pal.Coral)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val tl = Offset(sw / 2f, sw / 2f)
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(
                color = Pal.Ink.copy(alpha = 0.07f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = tl, size = sz, style = Stroke(sw)
            )
            if (anim > 0.001f) {
                rotate(-90f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(colors, center),
                        startAngle = 0f, sweepAngle = 360f * anim, useCenter = false,
                        topLeft = tl, size = sz, style = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
            }
        }
        content()
    }
}

@Composable
fun GradientBar(fraction: Float, modifier: Modifier = Modifier, done: Boolean = false) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "bar")
    Box(modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Pal.Ink.copy(alpha = 0.07f))) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        if (done) listOf(Pal.Mint, Color(0xFF8FE3C2)) else listOf(Pal.Coral, Pal.Amber)
                    )
                )
        )
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Pal.Coral, Pal.Amber)))
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Glass(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { i, label ->
                val sel = i == selected
                val fg by animateColorAsState(if (sel) Color.White else Pal.InkSoft, label = "fg")
                val bg by animateFloatAsState(if (sel) 1f else 0f, label = "bg")
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Pal.Coral.copy(alpha = bg))
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Glass(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = Pal.InkSoft, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Pal.InkSoft)
        }
    }
}

@Composable
fun NoticeCard(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Glass(Modifier.fillMaxWidth(), tint = Color(0xFFFFE9D6)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Pal.Ink)
            Spacer(Modifier.height(4.dp))
            Text(body, fontSize = 14.sp, color = Pal.InkSoft)
            if (actionLabel != null) {
                Spacer(Modifier.height(12.dp))
                GradientButton(actionLabel, onClick = onAction)
            }
        }
    }
}
