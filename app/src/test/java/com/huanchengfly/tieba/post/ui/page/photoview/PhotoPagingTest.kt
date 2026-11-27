package com.huanchengfly.tieba.post.ui.page.photoview

import com.huanchengfly.tieba.post.api.models.PicPageBean
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoPagingTest {
    @Test fun repeatedPictureIdsRepresentDifferentPositionsAcrossPages() {
        fun photo(index: Int) = PhotoViewItem("duplicate", "image$index", null, index, 123)
        val state = PhotoViewUiState(data = persistentListOf(photo(2), photo(3)), hasNext = true, hasPrev = true)
        val before = PhotoViewPartialChange.LoadPrev.Success(listOf(photo(1), photo(2)), false).reduce(state)
        val after = PhotoViewPartialChange.LoadMore.Success(listOf(photo(3), photo(4)), false).reduce(before)
        assertEquals((1..4).map(::photo), after.data)
    }

    @Test fun remoteAnchorKeepsTappedDuplicateAndFallsBackToScreenImage() {
        val source = "screen.png"
        fun local(index: Int) = PicItem("same", index, source, source, false, 0, 123)
        val view = PhotoViewData(
            data = LoadPicPageData(12, "本地", false, "index", "same", 2, 34, 123, source),
            picItems = listOf(local(1), local(2)),
            index = 1,
        )
        val original = PicPageBean.ImgInfoBean("same", null, null, "", "", "", "", "", "")
        val images = PicPageBean.ImgBean(original, null, original.copy(url = source))
        val page = PicPageBean(
            "0", PicPageBean.ForumBean("本地", "12"), "8",
            listOf(PicPageBean.PicBean("8", false, false, false, images, "123", null, null)),
        )
        val state = view.openRemotePage(page)
        assertEquals(listOf(7, 8), state.items.map { it.overallIndex })
        assertEquals(1, state.initialIndex)
        assertEquals(source, state.items[state.initialIndex].originUrl)
        assertEquals(123L, state.items[state.initialIndex].postId)
    }
    @Test fun mismatchedResponseKeepsTappedLocalPageAndAcceptsGenuineAlternateSizes() {
        val tapped = PicItem("selected-id", 2, "thumb.png", "original.png", false, 0, 123)
        val view = PhotoViewData(
            data = LoadPicPageData(12, "本地", false, "pb", "selected-id", 1, 34, 123, tapped.originUrl),
            picItems = listOf(tapped.copy(picId = "first-id", picIndex = 1, originUrl = "first.png"), tapped),
            index = 1,
        )
        fun page(id: String, thumbnail: String): PicPageBean {
            val full = PicPageBean.ImgInfoBean(id, null, null, "", "", "", "", "", "cdn-full.png")
            val img = PicPageBean.ImgBean(full, full.copy(originalSrc = thumbnail), null)
            return PicPageBean("0", PicPageBean.ForumBean("本地", "12"), "32",
                listOf(PicPageBean.PicBean("29", false, false, false, img, "123", null, null)))
        }
        val unrelated = view.openRemotePage(page("other", "other.png"))
        assertEquals(1, unrelated.initialIndex)
        assertEquals("original.png", unrelated.items[unrelated.initialIndex].originUrl)
        assertEquals(null, unrelated.loadPicPageData)
        for (related in listOf(page("selected-id", "other.png"), page("server-id", "thumb.png"))) {
            val result = view.openRemotePage(related)
            val selected = result.items[result.initialIndex]
            assertEquals(29, selected.overallIndex)
            assertEquals("cdn-full.png", selected.originUrl)
        }
    }
}
