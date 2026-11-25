package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail as MaterialNavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
internal fun DestinationIcon(item: NavigationItem, selected: Boolean) {
    BadgedBox(badge = {
        if (item.badge) {
            if (item.badgeText != null) {
                val count = item.badgeText.toIntOrNull()
                Badge { Text(if (count != null && count > 99) "99+" else item.badgeText) }
            } else Badge()
        }
    }) {
        Icon(
            painter = rememberAnimatedVectorPainter(item.icon(), atEnd = selected),
            modifier = Modifier.size(24.dp).testTag("navigation-icon-${item.id}"),
            contentDescription = null,
        )
    }
}

@Composable
internal fun Modifier.destinationBadgeDescription(item: NavigationItem): Modifier {
    if (!item.badge) return this
    val description = item.badgeText?.let { stringResource(R.string.navigation_unread_count, it) }
        ?: stringResource(R.string.navigation_unread)
    return semantics { stateDescription = description }
}

@Composable
internal fun NavigationDrawerContent(
    currentPosition: Int,
    onSelect: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition,
) {
    PermanentDrawerSheet {
        val account = LocalAccount.current
        Text(
            text = account?.let { it.nameShow ?: it.name } ?: stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
        )
        if (account != null) AccountNavIcon(spacer = false)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = if (navigationContentPosition == MainNavigationContentPosition.CENTER) Arrangement.Center else Arrangement.Top,
        ) {
            navigationItems.forEachIndexed { index, item ->
                val selected = index == currentPosition
                NavigationDrawerItem(
                    selected = selected,
                    onClick = { onSelect(index) },
                    icon = { DestinationIcon(item, selected) },
                    label = { Text(item.title(selected), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).destinationBadgeDescription(item),
                )
            }
        }
    }
}

@Composable
internal fun NavigationRail(
    currentPosition: Int,
    onSelect: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition,
) {
    MaterialNavigationRail(header = { AccountNavIcon(spacer = false) }) {
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (navigationContentPosition == MainNavigationContentPosition.CENTER) Arrangement.Center else Arrangement.Top,
        ) {
            navigationItems.forEachIndexed { index, item ->
                val selected = index == currentPosition
                NavigationRailItem(
                    modifier = Modifier.destinationBadgeDescription(item),
                    selected = selected,
                    onClick = { onSelect(index) },
                    icon = { DestinationIcon(item, selected) },
                    label = { Text(item.title(selected), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

@Immutable
data class NavigationItem @OptIn(ExperimentalAnimationGraphicsApi::class) constructor(
    val id: String,
    val icon: @Composable () -> AnimatedImageVector,
    val title: @Composable (selected: Boolean) -> String,
    val badge: Boolean = false,
    val badgeText: String? = null,
    val onClick: (() -> Unit)? = null,
    val content: @Composable () -> Unit = {},
)