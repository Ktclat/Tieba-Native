@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.photoview

import android.content.ClipData
import android.content.Intent
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.compose.rememberAsyncImageState
import com.github.panpf.sketch.request.LoadState
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.google.accompanist.systemuicontroller.SystemUiController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivityWithParcelable
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.toastShort
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
private fun ViewPhoto(
    imageUri: String,
    modifier: Modifier = Modifier,
    onTap: (offset: Offset) -> Unit = {},
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val state = rememberAsyncImageState()
        val progress by remember {
            derivedStateOf {
                state.progress?.let { transfer ->
                    if (transfer.totalLength > 0) (transfer.completedLength.toFloat() / transfer.totalLength).coerceIn(0f, 1f)
                    else null
                }
            }
        }
        val showProgress by remember {
            derivedStateOf {
                state.loadState is LoadState.Started
            }
        }
        SketchZoomAsyncImage(
            imageUri = imageUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            onTap = onTap,
            imageState = state,
        )
        if (showProgress) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                    val fraction = progress
                    if (fraction == null) {
                        LoadingIndicator()
                    } else {
                        CircularWavyProgressIndicator(progress = { fraction }, modifier = Modifier.size(56.dp))
                        Text(text = "${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PhotoActionBar(
    item: PhotoViewItem,
    total: Int,
    shareEnabled: Boolean,
    saveEnabled: Boolean,
    onShare: (String) -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A dark media overlay stays legible on both letterboxing and a full-bleed photo.
    // Apply the insets outside the surface; they must never enlarge its painted background.
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xD92A2A2C),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.height(48.dp).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (total > 1) {
                Text(
                    text = "${item.overallIndex} / $total",
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(
                onClick = { onShare(item.originUrl) },
                modifier = Modifier.size(48.dp),
                enabled = shareEnabled,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.Rounded.Share, stringResource(R.string.title_share_pic), Modifier.size(20.dp))
            }
            IconButton(
                onClick = { onSave(item.originUrl) },
                modifier = Modifier.size(48.dp),
                enabled = saveEnabled,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.Rounded.Download, stringResource(R.string.desc_download_pic), Modifier.size(20.dp))
            }
        }
    }
}

class PhotoViewActivity : BaseComposeActivityWithParcelable<PhotoViewData>() {
    private val viewModel: PhotoViewViewModel by viewModels {
        viewModelFactory { initializer { PhotoViewViewModel(checkNotNull(data), createSavedStateHandle()) } }
    }

    override val dataExtraKey: String = EXTRA_PHOTO_VIEW_DATA

    @Composable
    override fun Content(data: PhotoViewData) {
        val state by viewModel.uiState.collectAsState()
        val items = state.data
        val scope = rememberCoroutineScope()
        var sharing by remember { mutableStateOf(false) }
        var saving by remember { mutableStateOf(false) }

        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            if (items.isEmpty()) return@Surface
            val count by rememberUpdatedState(items.size)
            val pager = remember(viewModel) {
                PagerState(currentPage = viewModel.initialPage().coerceIn(items.indices)) { count }
            }
            val current by remember(pager, items) {
                derivedStateOf {
                    val page = pager.currentPage
                    val atIndex = items.getOrNull(page)
                    // A previous page may have arrived before the pager remeasures its anchor.
                    val displayedKey = pager.layoutInfo.visiblePagesInfo.firstOrNull { it.index == page }?.key
                    if (displayedKey == null || atIndex?.pageKey == displayedKey) atIndex
                    else items.firstOrNull { it.pageKey == displayedKey }
                }
            }
            LaunchedEffect(current, items.size, state.loadPicPageData, pager.isScrollInProgress) {
                if (!pager.isScrollInProgress) current?.let { page ->
                    viewModel.onViewed(page)
                    viewModel.loadAdjacent(page)
                }
            }

            Box(Modifier.fillMaxSize()) {
                HorizontalPager(state = pager, key = { items[it].pageKey }) { index ->
                    ViewPhoto(
                        imageUri = items[index].originUrl,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { finish() },
                    )
                }
                current?.let { item ->
                    PhotoActionBar(
                        item = item,
                        total = state.totalAmount,
                        shareEnabled = !sharing && !pager.isScrollInProgress,
                        saveEnabled = !saving && !pager.isScrollInProgress,
                        onShare = { url ->
                            if (!sharing) {
                                sharing = true
                                scope.launch {
                                    try {
                                        toastShort(R.string.toast_preparing_share_pic)
                                        val uri = ImageUtil.preparePhotoShare(this@PhotoViewActivity, url)
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = contentResolver.getType(uri) ?: "image/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            clipData = ClipData.newRawUri(null, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        startActivity(Intent.createChooser(send, getString(R.string.title_share_pic)))
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        toastShort(R.string.toast_photo_share_failed)
                                    } finally {
                                        sharing = false
                                    }
                                }
                            }
                        },
                        onSave = { url ->
                            if (!saving) {
                                saving = true
                                scope.launch {
                                    try {
                                        ImageUtil.savePhoto(this@PhotoViewActivity, url)?.let { path ->
                                            toastShort(R.string.toast_photo_saved, path)
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        toastShort(R.string.toast_photo_save_failed)
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
                    )
                }
            }
        }
    }

    override fun onCreateContent(systemUiController: SystemUiController) {
        systemUiController.isSystemBarsVisible = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.dispatchTouchEvent(ev)
        } catch (e: RuntimeException) {
            e.printStackTrace()
            true
        }
    }

    companion object {
        const val EXTRA_PHOTO_VIEW_DATA = "photo_view_data"
    }
}