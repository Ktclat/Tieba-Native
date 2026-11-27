package com.huanchengfly.tieba.post.ui.page.photoview

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import com.huanchengfly.tieba.post.api.models.PicPageBean
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class PhotoGalleryStartupTest {
    private val pictures = listOf("one", "two").mapIndexed { i, id ->
        PicItem(id, i + 1, "thumb-$id", "full-$id", true, 0)
    }
    private val source = PhotoViewData(
        LoadPicPageData(1, "本地", false, "pb", "one", 1, 123, 123, "full-one"), pictures, 0,
    )

    private fun page(vararg images: Pair<Int, String>): PicPageBean = PicPageBean(
        "0", PicPageBean.ForumBean("本地", "1"), "12",
        images.map { (position, id) ->
            val image = PicPageBean.ImgInfoBean(id, null, null, "", "", "", "", "", "full-$id")
            PicPageBean.PicBean(position.toString(), false, false, false,
                PicPageBean.ImgBean(image, null, null), null, null, null)
        },
    )

    @Test fun coldLocalLaunchHasClickedPhotoBeforeAnyCollectorOrScheduledTask() {
        val vm = PhotoViewViewModel(source.copy(data = null, index = 1), SavedStateHandle()) { _, _, _, _ ->
            error("Local gallery performed a network request")
        }
        assertEquals(2, vm.uiState.value.totalAmount)
        assertEquals("full-two", vm.uiState.value.data[vm.initialPage()].originUrl)
    }

    @Test fun delayedReplyRetainsSwipedPhotoAndRequestsEachBoundaryOnce() {
        val initial = CompletableDeferred<PicPageBean>()
        val next = CompletableDeferred<PicPageBean>()
        val calls = mutableListOf<Pair<String, Boolean>>()
        val saved = SavedStateHandle()
        val vm = PhotoViewViewModel(source, saved) { _, id, _, previous ->
            calls.add(id to previous)
            if (id == "one") initial.await() else next.await()
        }
        val local = vm.uiState.value.data[1]
        vm.onViewed(local)
        vm.loadAdjacent(local)
        assertEquals("full-two", local.originUrl)
        initial.complete(page(9 to "one", 10 to "two"))
        shadowOf(Looper.getMainLooper()).idle()
        val selected = vm.uiState.value.data[vm.initialPage()]
        assertEquals(local.pageKey, selected.pageKey)
        assertEquals(local.originUrl, selected.originUrl)
        assertEquals(10, selected.overallIndex)
        repeat(3) { vm.loadAdjacent(selected) }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("one" to false, "two" to false), calls)
        next.complete(page(10 to "two", 11 to "three", 12 to "four"))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(9, 10, 11, 12), vm.uiState.value.data.map { it.overallIndex })
        val remote = vm.uiState.value.data.last()
        vm.onViewed(remote)

        // Recreate after moving beyond the launch intent's two local pictures.
        val restoringRequest = CompletableDeferred<PicPageBean>()
        val restored = PhotoViewViewModel(source, SavedStateHandle(saved.keys().associateWith { saved.get<Any>(it) })) {
            _, id, index, _ ->
            assertEquals("four", id)
            assertEquals(12, index)
            restoringRequest.await()
        }
        assertEquals("full-four", restored.uiState.value.data[restored.initialPage()].originUrl)
        restoringRequest.complete(page(12 to "four"))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(12, restored.uiState.value.data[restored.initialPage()].overallIndex)
    }
}
