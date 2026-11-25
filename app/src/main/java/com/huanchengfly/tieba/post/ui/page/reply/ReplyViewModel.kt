package com.huanchengfly.tieba.post.ui.page.reply

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ReplyRequest
import com.huanchengfly.tieba.post.repository.ReplySubmission
import com.huanchengfly.tieba.post.repository.AddPostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

enum class ReplyPanelType {
    NONE,
    EMOJI,
    IMAGE,
    VOICE
}

@Stable
@HiltViewModel
class ReplyViewModel internal constructor(private val sender: ReplySubmission) :
    BaseViewModel<ReplyUiIntent, ReplyUiIntent, ReplyUiState, UiEvent>() {
    @Inject constructor() : this(ReplySubmission(AddPostRepository::submit))

    internal val submission get() = sender.state

    internal fun submit(request: ReplyRequest) = sender.submit(viewModelScope, request)

    override fun createInitialState() = ReplyUiState()

    override fun createPartialChangeProducer() =
        object : PartialChangeProducer<ReplyUiIntent, ReplyUiIntent, ReplyUiState> {
            override fun toPartialChangeFlow(intentFlow: Flow<ReplyUiIntent>) = intentFlow
        }
}

sealed interface ReplyUiIntent : UiIntent, PartialChange<ReplyUiState> {
    data class SwitchPanel(val panelType: ReplyPanelType) : ReplyUiIntent
    data class AddImage(val imageUris: List<String>) : ReplyUiIntent
    data class RemoveImage(val imageIndex: Int) : ReplyUiIntent
    data class ToggleIsOriginImage(val isOriginImage: Boolean) : ReplyUiIntent

    override fun reduce(oldState: ReplyUiState): ReplyUiState = when (this) {
        is SwitchPanel -> oldState.copy(replyPanelType = panelType)
        is AddImage -> oldState.copy(selectedImageList = (oldState.selectedImageList + imageUris).toImmutableList())
        is RemoveImage -> oldState.copy(selectedImageList = oldState.selectedImageList.filterIndexed { i, _ -> i != imageIndex }.toImmutableList())
        is ToggleIsOriginImage -> oldState.copy(isOriginImage = isOriginImage)
    }
}

data class ReplyUiState(
    val replyPanelType: ReplyPanelType = ReplyPanelType.NONE,
    val isOriginImage: Boolean = false,
    val selectedImageList: ImmutableList<String> = persistentListOf(),
) : UiState
