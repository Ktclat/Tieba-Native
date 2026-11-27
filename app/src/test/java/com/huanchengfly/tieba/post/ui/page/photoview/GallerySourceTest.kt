package com.huanchengfly.tieba.post.ui.page.photoview

import android.app.Application
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class GallerySourceTest {
    @get:Rule val appContext = AppContextRule()

    @Test fun postWithoutForumMetadataOpensTheTappedPictureWithItsSiblings() {
        val first = "https://example.org/a.jpg"
        val second = "https://example.org/b.png"
        val post = Post(id = 123, content = listOf(
            PbContent(type = 3, originSrc = first),
            PbContent(text = "第二张"),
            PbContent(type = 3, bigCdnSrc = second),
        ))
        val images = post.contentRenders.filterIsInstance<PicContentRender>()
        val data = requireNotNull(images.last().photoViewData)
        assertNull(data.data)
        assertEquals(1, data.index)
        assertEquals(listOf(first, second), data.picItems.map { it.originUrl })
        assertEquals(second, images.last().originUrl)
    }

    @Test fun feedWithoutOriginalUrlUsesTheSameLargePictureInPagerAndRequest() {
        val large = "https://example.org/b.png"
        val data = getPhotoViewData(
            medias = listOf(
                Media(originPic = "https://example.org/a.jpg"),
                Media(bigPic = large, srcPic = "https://example.org/thumb.png"),
            ),
            forumId = 1, forumName = "测试", threadId = 2, index = 1,
        )
        assertEquals(large, data.picItems[data.index].originUrl)
        assertEquals(large, data.data?.originUrl)
        assertEquals(data.picItems[data.index].picId, data.data?.picId)
    }
}
