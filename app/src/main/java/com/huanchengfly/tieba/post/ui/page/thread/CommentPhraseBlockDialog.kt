@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.style.TextOverflow
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuState
import com.huanchengfly.tieba.post.utils.LocalBlockRuleStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun CommentLongClickMenu(
    menuState: MenuState,
    phraseSource: (() -> String)?,
    menuContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    indication: Indication? = LocalIndication.current,
    content: @Composable () -> Unit,
) {
    var selecting by remember { mutableStateOf<String?>(null) }
    LongClickMenu(
        menuState = menuState,
        modifier = modifier,
        onClick = onClick,
        indication = indication,
        menuContent = {
            menuContent()
            val body = phraseSource?.invoke()
            if (!body.isNullOrBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.comment_select_phrase)) },
                    onClick = {
                        menuState.dismiss()
                        selecting = body
                    },
                )
            }
        },
        content = content,
    )
    selecting?.let { body ->
        val context = LocalContext.current
        CommentPhraseBlockDialog(
            body = body,
            onSave = { LocalBlockRuleStore.appendCommentLiteral(context, it) },
            onDismiss = { selecting = null },
        )
    }
}

@Composable
internal fun CommentPhraseBlockDialog(
    body: String,
    onSave: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var field by remember(body) { mutableStateOf(TextFieldValue(body)) }
    val phrase = field.getSelectedText().text
    var saving by remember { mutableStateOf(false) }
    var writeFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.comment_select_phrase)) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = {
                    if (!saving && it.text == body && it != field) {
                        field = it
                        writeFailed = false
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("comment-phrase-selection"),
                readOnly = true,
                minLines = 2,
                maxLines = 7,
                label = { Text(stringResource(R.string.comment_phrase_body)) },
                isError = writeFailed,
                supportingText = {
                    Text(
                        text = when {
                            writeFailed -> stringResource(R.string.block_rules_write_error)
                            phrase.isBlank() -> stringResource(R.string.comment_phrase_instructions)
                            else -> stringResource(R.string.comment_phrase_preview, phrase)
                        },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = !saving && phrase.isNotBlank(),
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    if (!saving) {
                        saving = true
                        scope.launch(Dispatchers.Main.immediate) {
                            try {
                                onSave(phrase)
                                onDismiss()
                            } catch (_: IOException) {
                                writeFailed = true
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
            ) { Text(stringResource(R.string.comment_phrase_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}
