package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@Composable
@ExperimentalMaterialApi
fun PrefsListItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    darkenOnDisable: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    secondaryText: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    textColor: Color = MaterialTheme.colors.onBackground,
    minimalHeight: Boolean = false,
    text: @Composable () -> Unit
) {

    val typography = MaterialTheme.typography

    val styledIcon = @Composable {
        val alpha = when {
            enabled || !darkenOnDisable -> ContentAlpha.high
            else -> ContentAlpha.disabled
        }
        CompositionLocalProvider(LocalContentAlpha provides alpha) {
            icon?.invoke()
        }
    }

    val styledText = applyTextStyle(
        typography.subtitle1.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 21.sp
        ),
        textColor,
        when {
            enabled || !darkenOnDisable -> ContentAlpha.high
            else -> ContentAlpha.disabled
        },
        text
    )!!
    val styledSecondaryText = applyTextStyle(
        typography.body2.copy(
            fontSize = 12.sp,
            lineHeight = 18.sp
        ),
        textColor,
        when {
            enabled || !darkenOnDisable -> ContentAlpha.medium
            else -> ContentAlpha.disabled
        },
        secondaryText
    )
    val styledTrailing = applyTextStyle(
        typography.caption,
        textColor,
        when {
            enabled || !darkenOnDisable -> ContentAlpha.high
            else -> ContentAlpha.disabled
        },
        trailing
    )

    AnyLine.CustomListItem(
        modifier = modifier,
        minimalHeight,
        styledIcon,
        styledSecondaryText,
        styledTrailing,
        styledText
    )
}

private object AnyLine {
    private val MinHeight = 58.dp
    private val MinHeightSmaller = 38.dp
    private val IconMinPaddedWidth = 46.dp
    private val ContentPadding = 16.dp
    private val VerticalPadding = 12.dp
    private val SingleLinePadding = 13.dp //used when no secondary text is supplied

    @Composable
    fun CustomListItem(
        modifier: Modifier = Modifier,
        minimalHeight: Boolean = true,
        icon: @Composable (() -> Unit)? = null,
        secondaryText: (@Composable (() -> Unit))? = null,
        trailing: @Composable (() -> Unit)? = null,
        text: @Composable (() -> Unit)
    ) {
        Row(
            modifier
                .heightIn(min = if (minimalHeight) MinHeightSmaller else MinHeight)
                .padding(
                    start = ContentPadding,
                    end = ContentPadding,
                    top = when {
                        secondaryText == null && !minimalHeight -> SingleLinePadding
                        else -> VerticalPadding
                    },
                    bottom = when {
                        minimalHeight -> 0.dp
                        secondaryText == null -> SingleLinePadding
                        else -> VerticalPadding
                    }
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (icon != null) {
                val minSize = IconMinPaddedWidth
                Box(
                    Modifier.sizeIn(minWidth = minSize, minHeight = minSize),
                    contentAlignment = Alignment.CenterStart
                ) { icon() }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                text()
                secondaryText?.invoke()
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing.invoke()
            }
        }
    }
}

private fun applyTextStyle(
    textStyle: TextStyle,
    textColor: Color,
    contentAlpha: Float,
    content: @Composable (() -> Unit)?
): @Composable (() -> Unit)? {
    if (content == null) return null
    return {
        val newTextStyle = textStyle.copy(
            color = textColor.copy(alpha = contentAlpha)
        )
        CompositionLocalProvider(
            LocalContentAlpha provides contentAlpha,
            LocalTextStyle provides newTextStyle
        ) {
            content()
        }
    }
}



