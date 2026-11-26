package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun UserHeaderPlaceholder(
    avatarSize: Dp
) {
    UserHeader(
        avatar = {
            AvatarPlaceholder(avatarSize)
        },
        name = {
            Text(
                text = "Username",
                modifier = Modifier.loadingPlaceholder(
                    visible = true,

                )
            )
        },
        desc = {
            Text(
                text = "Desc",
                modifier = Modifier.loadingPlaceholder(
                    visible = true,

                )
            )
        }
    )
}

@Composable
fun UserHeader(
    avatar: @Composable () -> Unit,
    name: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    desc: @Composable (() -> Unit)? = null,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = clickableModifier,
        ) {
            avatar()
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            ) {
                name()
            }
            if (desc != null) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    desc()
                }
            }
        }
        content?.invoke(this)
    }
}