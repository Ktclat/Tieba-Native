package com.huanchengfly.tieba.post.ui.page.photoview

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.PicPageBean
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.models.PhotoViewData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart

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

private fun PicItem.atPosition(position: Int) = PhotoViewItem(
    picId = picId,
    originUrl = originUrl.ifBlank { url },
    url = if (showOriginBtn) url else null,
    overallIndex = position,
    postId = postId,
)

private fun PhotoViewData.localPage() = PhotoViewPartialChange.Init.Success(
    items = picItems.mapIndexed { i, picture -> picture.atPosition(i + 1) },
    totalAmount = picItems.size,
    initialIndex = index,
    hasPrev = false,
    hasNext = false,
    loadPicPageData = null,
)

internal fun PhotoViewData.openRemotePage(page: PicPageBean): PhotoViewPartialChange.Init.Success {
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

    // Local position is relative to the tapped post. Server positions span the whole thread.
    val prefix = picItems.take(index.coerceAtLeast(0)).takeLast((selected.overallIndex - 1).coerceAtLeast(0))
    val firstLocal = selected.overallIndex - prefix.size
    val items = prefix.mapIndexed { i, picture -> picture.atPosition(firstLocal + i) }
        .takeWhile { it.overallIndex < fetched.first().overallIndex } + fetched
    val total = page.picAmount.toInt()
    return PhotoViewPartialChange.Init.Success(
        items = items,
        hasPrev = items.first().overallIndex > 1,
        hasNext = items.last().overallIndex < total,
        initialIndex = items.indexOfFirst { it.overallIndex == selected.overallIndex },
        totalAmount = total,
        loadPicPageData = request,
    )
}

class PhotoViewViewModel :
    BaseViewModel<PhotoViewUiIntent, PhotoViewPartialChange, PhotoViewUiState, PhotoViewUiEvent>() {
    override fun createInitialState(): PhotoViewUiState  = PhotoViewUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<PhotoViewUiIntent, PhotoViewPartialChange, PhotoViewUiState> = PhotoViewPartialChangeProducer

    private object PhotoViewPartialChangeProducer : PartialChangeProducer<PhotoViewUiIntent, PhotoViewPartialChange, PhotoViewUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<PhotoViewUiIntent>): Flow<PhotoViewPartialChange> =
            merge(
                intentFlow.filterIsInstance<PhotoViewUiIntent.Init>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<PhotoViewUiIntent.LoadPrev>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<PhotoViewUiIntent.LoadMore>()
                    .flatMapConcat { it.producePartialChange() },
            )

        private fun PhotoViewUiIntent.LoadPrev.producePartialChange(): Flow<PhotoViewPartialChange.LoadPrev> =
            TiebaApi.getInstance()
                .picPageFlow(
                    forumId = data.forumId.toString(),
                    forumName = data.forumName,
                    threadId = data.threadId.toString(),
                    seeLz = data.seeLz,
                    picId = picId,
                    picIndex = overallIndex.toString(),
                    objType = data.objType,
                    prev = true
                )
                .map<PicPageBean, PhotoViewPartialChange.LoadPrev> { picPageBean ->
                    val items = picPageBean.picList.toPhotoViewItems()
                    val hasPrev = items.first().overallIndex > 1
                    PhotoViewPartialChange.LoadPrev.Success(
                        hasPrev = hasPrev,
                        items = items
                    )
                }
                .onStart { emit(PhotoViewPartialChange.LoadPrev.Start) }
                .catch {
                    emit(PhotoViewPartialChange.LoadPrev.Failure(it))
                }

        private fun PhotoViewUiIntent.LoadMore.producePartialChange(): Flow<PhotoViewPartialChange.LoadMore> =
            TiebaApi.getInstance()
                .picPageFlow(
                    forumId = data.forumId.toString(),
                    forumName = data.forumName,
                    threadId = data.threadId.toString(),
                    seeLz = data.seeLz,
                    picId = picId,
                    picIndex = overallIndex.toString(),
                    objType = data.objType,
                    prev = false
                )
                .map<PicPageBean, PhotoViewPartialChange.LoadMore> { picPageBean ->
                    val items = picPageBean.picList.toPhotoViewItems()
                    val hasNext = items.last().overallIndex < picPageBean.picAmount.toInt()
                    PhotoViewPartialChange.LoadMore.Success(
                        hasNext = hasNext,
                        items = items
                    )
                }
                .onStart { emit(PhotoViewPartialChange.LoadMore.Start) }
                .catch {
                    emit(PhotoViewPartialChange.LoadMore.Failure(it))
                }

        private fun PhotoViewUiIntent.Init.producePartialChange(): Flow<PhotoViewPartialChange.Init> {
            val flow = if (data.data == null) {
                flowOf(data.localPage())
            } else {
                TiebaApi.getInstance()
                    .picPageFlow(
                        forumId = data.data.forumId.toString(),
                        forumName = data.data.forumName,
                        threadId = data.data.threadId.toString(),
                        seeLz = data.data.seeLz,
                        picId = data.data.picId,
                        picIndex = data.data.picIndex.toString(),
                        objType = data.data.objType,
                        prev = false
                    )
                    .map<PicPageBean, PhotoViewPartialChange.Init> { page -> data.openRemotePage(page) }
                    .catch {
                        emit(PhotoViewPartialChange.Init.Failure(data, it))
                    }
            }
            return flow
        }
    }
}

sealed interface PhotoViewUiIntent : UiIntent {
    data class Init(val data: PhotoViewData) : PhotoViewUiIntent

    data class LoadMore(
        val picId: String,
        val overallIndex: Int,
        val data: LoadPicPageData
    ) : PhotoViewUiIntent

    data class LoadPrev(
        val picId: String,
        val overallIndex: Int,
        val data: LoadPicPageData
    ) : PhotoViewUiIntent
}

sealed interface PhotoViewPartialChange : PartialChange<PhotoViewUiState> {
    sealed class Init : PhotoViewPartialChange {
        override fun reduce(oldState: PhotoViewUiState): PhotoViewUiState =
            when (this) {
                is Success -> oldState.copy(
                    data = items.toImmutableList(),
                    hasNext = hasNext,
                    hasPrev = hasPrev,
                    totalAmount = totalAmount,
                    initialIndex = initialIndex,
                    loadPicPageData = loadPicPageData,
                    isLoading = false
                )

                is Failure -> data.localPage().reduce(oldState)
            }

        data class Success(
            val items: List<PhotoViewItem>,
            val hasNext: Boolean,
            val hasPrev: Boolean,
            val totalAmount: Int,
            val initialIndex: Int,
            val loadPicPageData: LoadPicPageData?,
        ) : Init()

        data class Failure(
            val data: PhotoViewData,
            val error: Throwable
        ) : Init()
    }

    sealed class LoadPrev : PhotoViewPartialChange {
        override fun reduce(oldState: PhotoViewUiState): PhotoViewUiState =
            when (this) {
                Start -> oldState.copy(isLoading = true)

                is Success -> {
                    val first = oldState.data.firstOrNull()?.overallIndex ?: Int.MAX_VALUE
                    val previous = items.filter { it.overallIndex < first }
                    oldState.copy(
                        data = (previous + oldState.data).toImmutableList(),
                        hasPrev = hasPrev && previous.isNotEmpty(),
                        isLoading = false,
                    )
                }

                is Failure -> oldState.copy(isLoading = false)
            }

        data object Start : LoadPrev()

        data class Success(
            val items: List<PhotoViewItem>,
            val hasPrev: Boolean,
        ) : LoadPrev()

        data class Failure(
            val error: Throwable
        ) : LoadPrev()
    }

    sealed class LoadMore : PhotoViewPartialChange {
        override fun reduce(oldState: PhotoViewUiState): PhotoViewUiState =
            when (this) {
                Start -> oldState.copy(isLoading = true)

                is Success -> {
                    val last = oldState.data.lastOrNull()?.overallIndex ?: 0
                    val next = items.filter { it.overallIndex > last }
                    oldState.copy(
                        data = (oldState.data + next).toImmutableList(),
                        hasNext = hasNext && next.isNotEmpty(),
                        isLoading = false,
                    )
                }

                is Failure -> oldState.copy(isLoading = false)
            }

        data object Start : LoadMore()

        data class Success(
            val items: List<PhotoViewItem>,
            val hasNext: Boolean,
        ) : LoadMore()

        data class Failure(
            val error: Throwable
        ) : LoadMore()
    }
}

data class PhotoViewUiState(
    val isLoading: Boolean = false,
    val data: ImmutableList<PhotoViewItem> = persistentListOf(),
    val totalAmount: Int = 0,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false,
    val initialIndex: Int = 0,
    val loadPicPageData: LoadPicPageData? = null,
) : UiState

sealed interface PhotoViewUiEvent : UiEvent

data class PhotoViewItem(
    val picId: String,
    val originUrl: String,
    val url: String?,
    val overallIndex: Int,
    val postId: Long? = null,
)