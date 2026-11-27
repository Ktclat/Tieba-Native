@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar as M3TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalDisplayPreferences
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass.Companion.Compact
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.compose.calcStatusBarColor

@Composable
private fun toolbarContainerColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (LocalDisplayPreferences.current.toolbarPrimaryColor) colors.primary else colors.surface
}

@Composable
private fun toolbarContentColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (LocalDisplayPreferences.current.toolbarPrimaryColor) colors.onPrimary else colors.onSurface
}

@Composable
fun accountNavIconIfCompact(): (@Composable () -> Unit)? =
    if (LocalWindowSizeClass.current.widthSizeClass == Compact) (@Composable { AccountNavIcon() })
    else null

@Composable
fun AccountNavIcon(
    onClick: (() -> Unit)? = null,
    spacer: Boolean = true,
    size: Dp = Sizes.Small
) {
    val navigator = LocalNavigator.current
    val currentAccount = LocalAccount.current
    if (spacer) Spacer(modifier = Modifier.width(12.dp))
    if (currentAccount == null) {
        Image(
            painter = rememberDrawablePainter(
                drawable = ContextCompat.getDrawable(
                    LocalContext.current,
                    R.drawable.ic_launcher_new_round
                )
            ),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(size)
        )
    } else {
        val context = LocalContext.current
        val menuState = rememberMenuState()
        LongClickMenu(
            menuContent = {
                val allAccounts = AccountUtil.allAccounts
                allAccounts.forEach {
                    DropdownMenuItem(
                        onClick = { AccountUtil.switchAccount(context, it.id) },
                        text = { Text(text = it.nameShow ?: it.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(Sizes.Small)
                            ) {
                                Avatar(
                                    data = StringUtil.getAvatarUrl(it.portrait),
                                    contentDescription = stringResource(id = R.string.title_switch_account_long_press),
                                    size = Sizes.Small,
                                )
                                if (currentAccount.id == it.id) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ms_check_circle),
                                        contentDescription = stringResource(id = R.string.desc_current_account),
                                        tint = Color.White,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color = Color.Black.copy(0.35f))
                                            .padding(8.dp)
                                    )
                                }
                            }
                        },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DropdownMenuItem(
                    onClick = { navigator.navigate(LoginPageDestination) },
                    text = { Text(text = stringResource(R.string.title_new_account)) },
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ms_add),
                            contentDescription = stringResource(id = R.string.title_new_account),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(Sizes.Small)
                                .clip(CircleShape)
                                .background(color = MaterialTheme.colorScheme.secondaryContainer)
                                .padding(8.dp),
                        )
                    },
                )
            },
            menuState = menuState,
            onClick = onClick,
            shape = CircleShape
        ) {
            Avatar(
                data = StringUtil.getAvatarUrl(currentAccount.portrait),
                size = size,
                contentDescription = stringResource(id = R.string.title_switch_account_long_press)
            )
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    ProvideContentColor(color = toolbarContentColor()) {
        IconButton(shapes = IconButtonDefaults.shapes(), onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
fun BackNavigationIcon(onBackPressed: () -> Unit) {
    ProvideContentColor(color = toolbarContentColor()) {
        IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBackPressed) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ms_arrow_back),
                contentDescription = stringResource(id = R.string.button_back)
            )
        }
    }
}

@Deprecated(
    "Use the non deprecated overload",
    ReplaceWith(
        """TitleCentredToolbar(
                title = { Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                modifier = modifier,
                insets = insets,
                navigationIcon = navigationIcon,
                actions = actions,
                content = content
            )""",
        "androidx.compose.ui.text.font.FontWeight",
        "androidx.compose.material3.MaterialTheme",
        "androidx.compose.material3.Text"
    )
)
@Composable
fun TitleCentredToolbar(
    title: String,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TitleCentredToolbar(
        title = {
            Text(text = title)
        },
        modifier = modifier,
        insets = insets,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleCentredToolbar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    title()
                },
                navigationIcon = { navigationIcon?.invoke() },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = toolbarContainerColor(),
                    titleContentColor = toolbarContentColor(),
                    navigationIconContentColor = toolbarContentColor(),
                    actionIconContentColor = toolbarContentColor(),
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        modifier = modifier,
        insets = insets,
        content = content
    )
}

@Composable
fun Toolbar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Toolbar(
        title = {
            Text(text = title)
        },
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    title: @Composable (() -> Unit),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = toolbarContainerColor(),
    contentColor: Color = toolbarContentColor(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        topBar = {
            M3TopAppBar(
                title = { title() },
                actions = { actions() },
                navigationIcon = { navigationIcon?.invoke() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        content = content
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopAppBarContainer(
    topBar: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val statusBarModifier = if (insets) {
        Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
    } else {
        Modifier
    }
    val coroutineScope = rememberCoroutineScope()
    Column(modifier) {
        Spacer(
            modifier = statusBarModifier
                .fillMaxWidth()
                .background(color = toolbarContainerColor().calcStatusBarColor())
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onDoubleClick = {
                        coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop)
                    },
                    onClick = {},
                ),
            content = topBar
        )
        content?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = toolbarContainerColor()),
            ) {
                content()
            }
        }
    }
}