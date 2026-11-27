package com.huanchengfly.tieba.post.ui.page.photoview

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.PicPageBean
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.models.PhotoViewData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun PicPageBean.PicBean.variants() = listOfNotNull(img.original, img.screen, img.medium)

private fun PicPageBean.ImgInfoBean.sources() =
    listOf(originalSrc, bigCdnSrc, url, waterUrl).filterNot { it.isNullOrBlank() }

internal fun List<PicPageBean.PicBean>.toPhotoViewItems(): List<PhotoViewItem> =
    map { pic ->
        val variants = pic.variants()
        val source = variants.firstNotNullOfOrNull { it.sources().firstOrNull() }
        PhotoViewItem(
            picId = variants.firstNotNullOfOrNull { it.id?.takeIf(String::isNotBlank) }.orEmpty(),
            originUrl = requireNotNull(source) { "Missing picture URL" },
            url = if (pic.showOriginalBtn) pic.img.original?.bigCdnSrc else null,
            overallIndex = pic.overAllIndex.toInt(),
            postId = pic.postId?.toLongOrNull(),
        )
    }.distinctBy { it.overallIndex }

private fun PicItem.atPosition(position: Int, localIndex: Int) = PhotoViewItem(
    picId = picId,
    originUrl = originUrl.ifBlank { url },
    url = if (showOriginBtn) url else null,
    overallIndex = position,
    postId = postId,
    pageKey = -(localIndex + 1),
)

private fun PhotoViewData.localPage() = PhotoViewUiState(
    data = picItems.mapIndexed { i, picture -> picture.atPosition(i + 1, i) }.toImmutableList(),
    totalAmount = picItems.size,
    initialIndex = index,
    hasPrev = false,
    hasNext = false,
    loadPicPageData = null,
)

internal fun PhotoViewData.openRemotePage(page: PicPageBean): PhotoViewUiState {
    val request = requireNotNull(data)
    val tapped = picItems.getOrNull(index)
    val knownUrls = listOf(request.originUrl, tapped?.originUrl, tapped?.url).filterNotNull().filter { it.isNotBlank() }
    val matchingPositions = page.picList.filter { pic ->
        pic.variants().any { variant -> variant.sources().any { it in knownUrls } }
    }.mapTo(mutableSetOf()) { it.overAllIndex.toInt() }
    val fetched = page.picList.toPhotoViewItems()
    val matching = fetched.filter { it.overallIndex in matchingPositions }.ifEmpty {
        fetched.filter { request.picId.isNotBlank() && it.picId == request.picId }
    }
    val selected = matching.firstOrNull { it.overallIndex == request.picIndex }
        ?: matching.firstOrNull() ?: return localPage()

    // Retain every local key while switching to thread-wide numbering. If the server
    // disagrees with the post layout, keep the gallery the user is already viewing.
    val total = page.picAmount.toInt()
    val firstLocal = selected.overallIndex - index
    if (firstLocal < 1 || firstLocal + picItems.lastIndex > total) return localPage()
    val pages = fetched.associateByTo(sortedMapOf()) { it.overallIndex }
    val rawPages = page.picList.associateBy { it.overAllIndex.toInt() }
    picItems.forEachIndexed { i, picture ->
        val position = firstLocal + i
        val local = picture.atPosition(position, i)
        val remote = pages[position]
        if (remote == null) {
            pages[position] = local
        } else {
            val same = i == index || (picture.picId.isNotBlank() && picture.picId == remote.picId) ||
                rawPages.getValue(position).variants().any { variant ->
                    variant.sources().any { it == picture.originUrl || it == picture.url }
                }
            if (!same) return localPage()
            pages[position] = remote.copy(pageKey = local.pageKey)
        }
    }
    val items = pages.values.toList()
    return PhotoViewUiState(
        data = items.toImmutableList(),
        hasPrev = items.first().overallIndex > 1,
        hasNext = items.last().overallIndex < total,
        initialIndex = items.indexOfFirst { it.overallIndex == selected.overallIndex },
        totalAmount = total,
        loadPicPageData = request,
    )
}

private suspend fun fetchPage(
    request: LoadPicPageData, picId: String, index: Int, previous: Boolean,
): PicPageBean = withContext(Dispatchers.IO) {
    TiebaApi.getInstance().picPageFlow(
        forumId = request.forumId.toString(), forumName = request.forumName,
        threadId = request.threadId.toString(), seeLz = request.seeLz,
        picId = picId, picIndex = index.toString(), objType = request.objType, prev = previous,
    ).first()
}

internal class PhotoViewViewModel(
    source: PhotoViewData,
    private val saved: SavedStateHandle,
    private val fetch: suspend (LoadPicPageData, String, Int, Boolean) -> PicPageBean = ::fetchPage,
) : ViewModel() {
    private val restoredPhoto = saved.get<PicItem>("viewedPhoto")
    // A remote-only image can still be displayed immediately after process recreation.
    private val launch = if (restoredPhoto != null && saved.get<Boolean>("remoteOnly") == true) {
        source.copy(
            data = source.data?.copy(picId = restoredPhoto.picId, picIndex = restoredPhoto.picIndex,
                originUrl = restoredPhoto.originUrl, postId = restoredPhoto.postId ?: source.data.postId),
            picItems = listOf(restoredPhoto), index = 0,
        )
    } else source
    private val state = MutableStateFlow(launch.localPage())
    val uiState = state.asStateFlow()
    private val pendingEdges = mutableSetOf<Boolean>()
    private val standaloneRestoredPhoto = launch !== source

    init {
        launch.data?.let { request ->
            viewModelScope.launch {
                try {
                    val page = fetch(request, request.picId, request.picIndex, false)
                    state.value = launch.openRemotePage(page)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Local pictures and their actions remain available offline.
                }
            }
        }
    }

    fun initialPage(): Int {
        val items = state.value.data
        val key = saved.get<Int>("viewedKey")
        val index = items.indexOfFirst { it.pageKey == key }
        return if (index >= 0) index else state.value.initialIndex
    }

    fun onViewed(item: PhotoViewItem) {
        saved["viewedKey"] = item.pageKey
        saved["remoteOnly"] = item.pageKey > 0 || standaloneRestoredPhoto
        saved["viewedPhoto"] = PicItem(item.picId, item.overallIndex, item.url ?: item.originUrl, item.originUrl,
            item.url != null, 0, item.postId)
    }

    fun loadAdjacent(item: PhotoViewItem) {
        val now = state.value
        val request = now.loadPicPageData ?: return
        if (now.hasPrev && item.pageKey == now.data.firstOrNull()?.pageKey) load(request, item, true)
        if (now.hasNext && item.pageKey == now.data.lastOrNull()?.pageKey) load(request, item, false)
    }

    private fun load(request: LoadPicPageData, item: PhotoViewItem, previous: Boolean) {
        if (!pendingEdges.add(previous)) return
        viewModelScope.launch {
            try {
                val page = fetch(request, item.picId, item.overallIndex, previous)
                val pictures = page.picList.toPhotoViewItems()
                state.value = state.value.withAdjacent(pictures, page.picAmount.toInt(), previous)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Returning to this boundary can retry without disturbing the current page.
            } finally {
                pendingEdges.remove(previous)
            }
        }
    }
}

internal fun PhotoViewUiState.withAdjacent(pictures: List<PhotoViewItem>, total: Int, previous: Boolean): PhotoViewUiState {
    return if (previous) {
        val first = data.firstOrNull()?.overallIndex ?: Int.MAX_VALUE
        val earlier = pictures.filter { it.overallIndex < first }
        copy(data = (earlier + data).toImmutableList(), hasPrev = (earlier.firstOrNull()?.overallIndex ?: 1) > 1)
    } else {
        val last = data.lastOrNull()?.overallIndex ?: 0
        val later = pictures.filter { it.overallIndex > last }
        copy(data = (data + later).toImmutableList(), hasNext = (later.lastOrNull()?.overallIndex ?: Int.MAX_VALUE) < total)
    }
}

data class PhotoViewUiState(
    val data: ImmutableList<PhotoViewItem> = persistentListOf(),
    val totalAmount: Int = 0,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false,
    val initialIndex: Int = 0,
    val loadPicPageData: LoadPicPageData? = null,
)

data class PhotoViewItem(
    val picId: String,
    val originUrl: String,
    val url: String?,
    val overallIndex: Int,
    val postId: Long? = null,
    val pageKey: Int = overallIndex,
)