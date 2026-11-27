@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.settings.block

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.BlockRuleError
import com.huanchengfly.tieba.post.utils.BlockRuleTarget
import com.huanchengfly.tieba.post.utils.LocalBlockRuleStore
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@get:StringRes
internal val BlockRuleTarget.titleRes: Int
    get() = when (this) {
        BlockRuleTarget.FORUM -> R.string.block_rules_forum
        BlockRuleTarget.USER -> R.string.block_rules_user
        BlockRuleTarget.COMMENT -> R.string.block_rules_comment
    }

@get:DrawableRes
internal val BlockRuleTarget.iconRes: Int
    get() = when (this) {
        BlockRuleTarget.FORUM -> R.drawable.ms_forum
        BlockRuleTarget.USER -> R.drawable.ms_person
        BlockRuleTarget.COMMENT -> R.drawable.ms_chat_bubble
    }

@get:StringRes
private val BlockRuleTarget.exampleRes: Int
    get() = when (this) {
        BlockRuleTarget.FORUM -> R.string.block_rules_forum_example
        BlockRuleTarget.USER -> R.string.block_rules_user_example
        BlockRuleTarget.COMMENT -> R.string.block_rules_comment_example
    }

@Destination
@Composable
fun BlockRuleEditorPage(target: BlockRuleTarget, navigator: DestinationsNavigator) {
    val context = LocalContext.current
    BlockRuleEditor(
        target = target,
        savedText = LocalContentBlockRules.current.text(target),
        onSave = { LocalBlockRuleStore.save(context, target, it) },
        onBack = { navigator.navigateUp() },
    )
}

@Composable
internal fun BlockRuleEditor(
    target: BlockRuleTarget,
    savedText: String,
    onSave: suspend (String) -> Unit,
    onBack: () -> Unit,
) {
    var draft by rememberSaveable(target) { mutableStateOf<String?>(null) }
    val text = draft ?: savedText
    val validation by produceState<Pair<String, BlockRuleError?>?>(null, text) {
        value = text to withContext(Dispatchers.Default) { LocalBlockRules.validate(text) }
    }
    val currentCheck = validation?.takeIf { it.first == text }
    val error = currentCheck?.second
    var saving by remember { mutableStateOf(false) }
    var writeFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = saving) {}

    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = { Text(stringResource(target.titleRes)) },
                navigationIcon = { BackNavigationIcon { if (!saving) onBack() } },
                actions = {
                    TextButton(
                        enabled = text != savedText && currentCheck != null && error == null && !saving,
                        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            if (!saving) {
                                saving = true
                                scope.launch(Dispatchers.Main.immediate) {
                                    try {
                                        onSave(text)
                                        onBack()
                                    } catch (_: IOException) {
                                        writeFailed = true
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        },
                    ) { Text(stringResource(R.string.block_rules_save)) }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .imePadding().navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.block_rules_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        draft = it
                        writeFailed = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.block_rules_field)) },
                    enabled = !saving,
                    minLines = 5,
                    maxLines = 12,
                    isError = error != null,
                    supportingText = {
                        Text(
                            if (error != null) stringResource(R.string.block_rules_invalid, error.line, error.message)
                            else stringResource(target.exampleRes),
                        )
                    },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                )
                if (writeFailed) {
                    Text(
                        stringResource(R.string.block_rules_write_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
