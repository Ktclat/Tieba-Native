package com.huanchengfly.tieba.post.ui.page.reply

import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalAppDarkTheme
import androidx.compose.ui.graphics.toArgb
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.content.res.ColorStateList
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.key
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.repository.ReplyRequest
import com.huanchengfly.tieba.post.repository.ReplySubmissionState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import com.github.panpf.sketch.compose.AsyncImage
import com.google.android.material.textfield.TextInputLayout
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.pxToDpFloat
import com.huanchengfly.tieba.post.toMD5
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.EMOJI
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.IMAGE
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.NONE
import com.huanchengfly.tieba.post.ui.utils.imeNestedScroll
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.edittext.widget.UndoableEditText
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.hideKeyboard
import com.huanchengfly.tieba.post.utils.showKeyboard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.litepal.extension.deleteAllAsync
import org.litepal.extension.findFirstAsync
import java.util.UUID
import kotlin.math.max

data class ReplyArgs(
    val forumId: Long,
    val forumName: String,
    val threadId: Long,
    val postId: Long? = null,
    val subPostId: Long? = null,
    val replyUserId: Long? = null,
    val replyUserName: String? = null,
    val replyUserPortrait: String? = null,
    val tbs: String? = null,
)

internal fun DestinationsNavigator.openReply(args: ReplyArgs, accountId: String?) {
    navigate(ReplyPageDestination(
        forumId = args.forumId, forumName = args.forumName, threadId = args.threadId,
        postId = args.postId, subPostId = args.subPostId, replyUserId = args.replyUserId,
        replyUserName = args.replyUserName, replyUserPortrait = args.replyUserPortrait,
        tbs = args.tbs.takeIf { accountId != null }, tbsAccountId = accountId,
    ))
    if (accountId == null) navigate(LoginPageDestination)
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    FlowPreview::class
)
@Composable
internal fun ReplyPageContent(
    viewModel: ReplyViewModel,
    onBack: () -> Unit,
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserId: Long? = null,
    replyUserName: String? = null,
    replyUserPortrait: String? = null,
    tbs: String? = null,
    accountId: String,
) {
    val draft = remember(accountId, threadId, postId, subPostId) {
        ReplyDrafts.target(accountId, threadId, postId, subPostId)
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val submission by viewModel.submission.collectAsState()
    val isReplying = submission is ReplySubmissionState.Sending || submission is ReplySubmissionState.Sent
    val curKeyboardType by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::replyPanelType,
        initial = NONE
    )
    val selectedImageList by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::selectedImageList,
        initial = persistentListOf()
    )
    val isOriginImage by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isOriginImage,
        initial = false
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    var initialText by remember { mutableStateOf("") }
    var waitEditTextToSet by remember { mutableStateOf(false) }
    var editTextView by remember { mutableStateOf<UndoableEditText?>(null) }
    fun getText(): String {
        return editTextView?.text?.toString().orEmpty()
    }

    fun setText(text: String) {
        if (editTextView != null) {
            editTextView?.setText(StringUtil.getEmoticonContent(editTextView!!, text))
            editTextView?.setSelection(text.length)
        } else {
            initialText = text
            waitEditTextToSet = true
        }
    }

    fun insertEmoticon(text: String) {
        editTextView?.apply {
            val start = selectionStart
            this.text?.insert(start, text)
            this.setText(StringUtil.getEmoticonContent(this, this.text))
            setSelection(start + text.length)
        }
    }

    var curText by remember(draft) { mutableStateOf("") }
    var edited by remember(draft) { mutableStateOf(false) }
    LaunchedEffect(draft) {
        val saved = ReplyDrafts.read(draft)
        if (!edited && !saved.isNullOrEmpty()) setText(saved)
    }
    val textLength = curText.length
    val onSent = submission as? ReplySubmissionState.Sent
    LaunchedEffect(onSent) {
        if (onSent != null) {
            ReplyDrafts.clear(draft)
            if (onSent.expInc.isEmpty()) context.toastShort(R.string.toast_reply_success_default)
            else context.toastShort(R.string.toast_reply_success, onSent.expInc)
            onBack()
        }
    }
    BackHandler(enabled = isReplying) {}

    var closingPanel by remember { mutableStateOf(false) }
    var startClosingAnimation by remember { mutableStateOf(false) }

    fun showKeyboard() {
        editTextView?.apply {
            showKeyboard(context, this)
            requestFocus()
        }
        keyboardController?.show()
    }

    fun hideKeyboard() {
        editTextView?.apply {
            hideKeyboard(context, this)
            clearFocus()
        }
        keyboardController?.hide()
    }

    fun switchToPanel(type: ReplyPanelType) {
        if (curKeyboardType == type || type == NONE) {
            if (curKeyboardType != NONE) {
                showKeyboard()
                closingPanel = true
                startClosingAnimation = false
            }
            viewModel.send(ReplyUiIntent.SwitchPanel(NONE))
        } else {
            hideKeyboard()
            viewModel.send(ReplyUiIntent.SwitchPanel(type))
        }
    }

    MyBackHandler(
        enabled = !isReplying && curKeyboardType != NONE,
        currentScreen = ReplyPageDestination
    ) {
        switchToPanel(NONE)
    }

    val density = LocalDensity.current
    val imeInset = WindowInsets.ime
    val imeAnimationTargetInset = WindowInsets.imeAnimationTarget

    val imeCurrentHeight by produceState(initialValue = 0, imeInset, density) {
        snapshotFlow { imeInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationTargetHeight by produceState(
        initialValue = 0,
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationEnd by remember { derivedStateOf { imeCurrentHeight == imeAnimationTargetHeight } }
    val imeVisibleHeightPx by produceState(
        initialValue = remember { context.appPreferences.imeHeight },
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .filter { it > 0 }
            .distinctUntilChanged()
            .collect {
                context.appPreferences.imeHeight = it
                value = it
            }
    }

    val panelHeight by remember {
        derivedStateOf { max(imeVisibleHeightPx.pxToDpFloat(), 150f).dp }
    }

    LaunchedEffect(closingPanel, imeAnimationEnd) {
        if (closingPanel) {
            if (!startClosingAnimation && !imeAnimationEnd) {
                startClosingAnimation = true
            } else if (startClosingAnimation && imeAnimationEnd) {
                closingPanel = false
                startClosingAnimation = false
            }
        }
    }

    val canSend = !isReplying && (curText.isNotBlank() || selectedImageList.isNotEmpty())
    fun submit() {
        viewModel.submit(ReplyRequest(
            content = getText(), forumId = forumId, forumName = forumName, threadId = threadId,
            tbs = tbs.orEmpty(), postId = postId, subPostId = subPostId, replyUserId = replyUserId,
            replyUserName = replyUserName, replyUserPortrait = replyUserPortrait,
            imageUris = selectedImageList, isOriginImage = isOriginImage,
        ))
    }

    val textFieldScrollState = rememberScrollState()

    val parentModifier = Modifier.fillMaxSize().navigationBarsPadding().then(
        if (curKeyboardType != NONE || closingPanel) Modifier.consumeWindowInsets(WindowInsets.ime)
        else Modifier
    )

    Column(modifier = parentModifier) {
        TopAppBar(
            title = { Text(stringResource(R.string.title_reply)) },
            navigationIcon = { BackNavigationIcon(onBackPressed = { if (!isReplying) onBack() }) },
            actions = {
                Text(
                    "$textLength",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imeNestedScroll(textFieldScrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(textFieldScrollState)
            ) {
                val dark = LocalAppDarkTheme.current
                val colors = MaterialTheme.colorScheme
                val borderColors = remember(colors) {
                    ColorStateList(
                        arrayOf(
                            intArrayOf(-android.R.attr.state_enabled),
                            intArrayOf(android.R.attr.state_focused),
                            intArrayOf(android.R.attr.state_hovered),
                            intArrayOf(),
                        ),
                        intArrayOf(
                            colors.onSurface.copy(alpha = 0.12f).toArgb(),
                            colors.primary.toArgb(), colors.onSurface.toArgb(), colors.outline.toArgb(),
                        ),
                    )
                }
                val labelColor = remember(colors) { ColorStateList.valueOf(colors.onSurfaceVariant.toArgb()) }
                val focusColor = remember(colors) { ColorStateList.valueOf(colors.primary.toArgb()) }
                val bodySize = with(density) { MaterialTheme.typography.bodyLarge.fontSize.toPx() }
                AndroidView(
                    factory = { ctx ->
                        val themed = ContextThemeWrapper(ctx, if (dark) R.style.AppTheme_Dark else R.style.AppTheme)
                        val input = View.inflate(themed, R.layout.layout_reply_edit_text, null) as TextInputLayout
                        input.findViewById<UndoableEditText>(R.id.edit_text).apply {
                            editTextView = this
                            if (postId != null && postId > 0L && !replyUserName.isNullOrBlank()) {
                                input.hint = ctx.getString(R.string.hint_reply, replyUserName)
                            }
                            setOnFocusChangeListener { _, hasFocus ->
                                if (hasFocus) {
                                    switchToPanel(NONE)
                                }
                            }
                            addTextChangedListener(
                                afterTextChanged = {
                                    val text = it?.toString().orEmpty()
                                    if (viewModel.submission.value !is ReplySubmissionState.Sent) {
                                        edited = true
                                        curText = text
                                        ReplyDrafts.save(draft, text)
                                    }
                                }
                            )
                            if (waitEditTextToSet) {
                                waitEditTextToSet = false
                                this.setText(StringUtil.getEmoticonContent(this, initialText))
                                this.setSelection(initialText.length)
                            }
                        }
                        input
                    },
                    update = { input ->
                        input.setBoxStrokeColorStateList(borderColors)
                        input.defaultHintTextColor = labelColor
                        input.hintTextColor = focusColor
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            input.cursorColor = focusColor
                        }
                        input.editText?.apply {
                            isEnabled = !isReplying
                            setTextColor(colors.onSurface.toArgb())
                            setHintTextColor(labelColor)
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, bodySize)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top)
                )
            }
        }
        val failure = submission as? ReplySubmissionState.Failed
        if (failure != null) {
            Text(
                stringResource(R.string.toast_reply_failed, failure.errorCode, failure.errorMessage),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { switchToPanel(EMOJI) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEmotions,
                    contentDescription = stringResource(id = R.string.insert_emotions),
                    modifier = Modifier.size(24.dp)
                )
            }
            if (postId == null || postId == 0L) {
                IconButton(
                    onClick = { switchToPanel(IMAGE) },
                ) {
                    BadgedBox(
                        badge = {
                            if (selectedImageList.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(
                                        text = "${selectedImageList.size}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.InsertPhoto,
                            contentDescription = stringResource(id = R.string.insert_photo),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
//            IconButton(
//                onClick = { switchToPanel(VOICE) },
//                modifier = Modifier.size(24.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Outlined.KeyboardVoice,
//                    contentDescription = stringResource(id = R.string.insert_voice),
//                    modifier = Modifier.size(24.dp)
//                )
//            }
            Spacer(modifier = Modifier.weight(1f))
            if (isReplying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                FilledIconButton(
                    onClick = ::submit,
                    enabled = canSend,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(if (failure == null) R.string.send_reply else R.string.reply_retry),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.ime)
        )
        if (curKeyboardType != NONE) {
            Column(modifier = Modifier.height(panelHeight)) {
                when (curKeyboardType) {
                    EMOJI -> {
                        EmoticonPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            onEmoticonClick = { emoticon ->
                                insertEmoticon("#(${emoticon.name})")
                            }
                        )
                    }

                    IMAGE -> {
                        ImagePanel(
                            selectedImages = selectedImageList,
                            onNewImageSelected = { uris ->
                                viewModel.send(ReplyUiIntent.AddImage(uris.map { it.toString() }))
                            },
                            onRemoveImage = {
                                viewModel.send(ReplyUiIntent.RemoveImage(it))
                            },
                            isOriginImage = isOriginImage,
                            onIsOriginImageChange = {
                                viewModel.send(ReplyUiIntent.ToggleIsOriginImage(it))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        )
                    }

                    else -> {}
                }
            }
        } else if (closingPanel) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
            )
        }
    }

    DisposableEffect(editTextView) {
        if (editTextView != null) {
            showKeyboard()
        }

        onDispose {
            if (editTextView != null) {
                hideKeyboard()
            }
        }
    }

    fun getDispatchUri(): Uri {
        return if (postId != null) {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=comment_lzl_cut_guide&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2&hightlight_anchor_pid=${postId}&is_anchor_to_comment=1&comment_sort_type=0&fr=bpush&tid=${threadId}")
        } else {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=pb_reply&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2-99999_9&fr=bpush&tid=${threadId}")
        }
    }

    fun launchOfficialApp() {
        val intent = Intent(Intent.ACTION_VIEW).setData(getDispatchUri())
        val resolveInfos =
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ).filter { it.activityInfo.packageName != context.packageName }
        try {
            if (resolveInfos.isNotEmpty()) {
                context.startActivity(intent)
            } else {
                context.toastShort(R.string.toast_official_client_not_install)
            }
        } catch (e: ActivityNotFoundException) {
            context.toastShort(R.string.toast_official_client_not_install)
        }
    }

    val warningDialogState = rememberDialogState()
    Dialog(
        dialogState = warningDialogState,
        title = { Text(text = stringResource(id = R.string.title_dialog_reply_warning)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_official_client_reply),
                onClick = { launchOfficialApp() }
            )
            DialogNegativeButton(text = stringResource(id = R.string.btn_continue_reply))
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_cancel_reply),
                onClick = { onBack() }
            )
        },
    ) {
        Text(stringResource(R.string.message_dialog_reply_warning))
    }

    LaunchedEffect(Unit) {
        if (context.appPreferences.postOrReplyWarning) {
            warningDialogState.show()
        }
    }
}

// TODO: 将软键盘状态相关逻辑抽离出来
@Destination
@Composable
fun ReplyPage(
    navigator: DestinationsNavigator,
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserId: Long? = null,
    replyUserName: String? = null,
    replyUserPortrait: String? = null,
    tbs: String? = null,
    tbsAccountId: String? = null,
    viewModel: ReplyViewModel = pageViewModel(),
) {
    val account = AccountUtil.LocalAccount.current
    if (account == null) {
        ReplyLoginPrompt(onLogin = { navigator.navigate(LoginPageDestination) }, onBack = { navigator.navigateUp() })
        return
    }
    key(account.uid) { Surface(modifier = Modifier.fillMaxSize()) {
        ReplyPageContent(
            viewModel = viewModel,
            onBack = { navigator.navigateUp() },
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            replyUserId = replyUserId,
            replyUserName = replyUserName,
            replyUserPortrait = replyUserPortrait,
            tbs = tbs?.takeIf { it.isNotBlank() && tbsAccountId == account.uid } ?: account.tbs,
            accountId = account.uid,
        )
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReplyLoginPrompt(onLogin: () -> Unit, onBack: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column {
            TopAppBar(title = { Text(stringResource(R.string.title_reply)) },
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) })
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onLogin) { Text(stringResource(R.string.login_to_reply)) }
            }
        }
    }
}

@Composable
private fun EmoticonPanel(
    modifier: Modifier = Modifier,
    onEmoticonClick: (Emoticon) -> Unit,
) {
    val emoticons = remember {
        EmoticonManager.getAllEmoticon()
            .filter { it.name.isNotEmpty() }
    }

    Column(
        modifier = modifier
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            items(emoticons) { emoticon ->
                Image(
                    painter = rememberDrawablePainter(
                        drawable = EmoticonManager.getEmoticonDrawable(
                            LocalContext.current,
                            emoticon.id
                        )
                    ),
                    contentDescription = stringResource(
                        id = R.string.emoticon,
                        emoticon.name
                    ),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                        .clickable { onEmoticonClick(emoticon) },
                )
            }
        }
    }
}

@Composable
private fun ImagePanel(
    selectedImages: ImmutableList<String>,
    onNewImageSelected: (List<Uri>) -> Unit,
    onRemoveImage: (Int) -> Unit,
    isOriginImage: Boolean,
    onIsOriginImageChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = remember { UUID.randomUUID().toString() }
    val coroutineScope = rememberCoroutineScope()
    onGlobalEvent<GlobalEvent.SelectedImages>(
        coroutineScope,
        filter = { it.id == id }
    ) {
        onNewImageSelected(it.images)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxHeight(0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            item {
//                Spacer(modifier = Modifier.width(16.dp))
//            }
            itemsIndexed(selectedImages) { index, imageUri ->
                Box {
                    AsyncImage(
                        imageUri = imageUri,
                        contentDescription = stringResource(id = R.string.desc_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                    )
                    IconButton(
                        onClick = { onRemoveImage(index) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.desc_remove_image)
                        )
                    }
                }
            }
            if (selectedImages.size < 9) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable {
                                coroutineScope.emitGlobalEvent(
                                    GlobalEvent.StartSelectImages(
                                        id,
                                        9 - selectedImages.size,
                                        PickMediasRequest.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.desc_add_image),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .toggleable(value = isOriginImage, role = Role.Checkbox, onValueChange = onIsOriginImageChange)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isOriginImage, onCheckedChange = null)
            Text(text = stringResource(id = R.string.origin_image))
        }
    }
}