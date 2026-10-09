package com.hazuny.noshnote.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
internal fun uiText(@StringRes id: Int, vararg formatArgs: Any): String =
    stringResource(id = id, formatArgs = formatArgs)

/** Shrinks localized text only when it would otherwise exceed its allotted line count. */
@Composable
internal fun AutoFitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    contentAlignment: Alignment = Alignment.Center,
    maxLines: Int = 2,
    softWrap: Boolean = true,
    minFontSize: TextUnit = 9.sp,
) {
    val textMeasurer = rememberTextMeasurer()
    val resolvedStyle = style.copy(
        fontWeight = fontWeight ?: style.fontWeight,
        textAlign = textAlign ?: style.textAlign,
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = contentAlignment) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }
        val baseFontSize = if (resolvedStyle.fontSize != TextUnit.Unspecified) resolvedStyle.fontSize.value else 14f
        val minimum = minFontSize.value.coerceAtMost(baseFontSize)
        val fontSize = remember(text, resolvedStyle, widthPx, heightPx, maxLines, softWrap, minimum) {
            val candidates = generateSequence(baseFontSize) { current ->
                (current - 0.5f).takeIf { it >= minimum }
            }.toList() + minimum
            candidates.firstOrNull { candidate ->
                val layout = textMeasurer.measure(
                    text = text,
                    style = resolvedStyle.copy(fontSize = candidate.sp),
                    constraints = Constraints(maxWidth = widthPx, maxHeight = heightPx),
                    maxLines = maxLines,
                    softWrap = softWrap,
                    overflow = TextOverflow.Clip,
                )
                layout.lineCount <= maxLines && !layout.didOverflowHeight && !layout.didOverflowWidth
            } ?: minimum
        }

        Text(
            text = text,
            style = resolvedStyle.copy(fontSize = fontSize.sp),
            color = color,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = TextOverflow.Clip,
        )
    }
}
