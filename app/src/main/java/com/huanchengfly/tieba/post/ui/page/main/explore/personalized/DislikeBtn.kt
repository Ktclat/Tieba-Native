@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.api.models.protos.personalized.ThreadPersonalized
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun Dislike(
    personalized: ImmutableHolder<ThreadPersonalized>,
    onDislike: (clickTime: Long, reasons: ImmutableList<ImmutableHolder<DislikeReason>>) -> Unit,
) {
    val recommendation = personalized.get()
    var openedAt by rememberSaveable(recommendation) { mutableStateOf<Long?>(null) }
    var selectedIndices by rememberSaveable(recommendation) { mutableStateOf(intArrayOf()) }
    IconButton(
        onClick = {
            selectedIndices = intArrayOf()
            openedAt = System.currentTimeMillis()
        },
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ms_keyboard_arrow_down),
            contentDescription = stringResource(R.string.button_submit_dislike),
        )
    }

    val clickTime = openedAt ?: return
    val reasons = personalized.getImmutableList { dislikeResource }
    AlertDialog(
        onDismissRequest = { openedAt = null },
        title = { Text(stringResource(R.string.title_dislike)) },
        text = {
            FlowRow(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                reasons.forEachIndexed { index, reason ->
                    val selected = index in selectedIndices
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedIndices = if (selected) {
                                selectedIndices.filter { it != index }.toIntArray()
                            } else {
                                selectedIndices + index
                            }
                        },
                        label = { Text(reason.get { dislikeReason }) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    openedAt = null
                    val selected = selectedIndices.toList().mapNotNull { reasons.getOrNull(it) }
                    onDislike(clickTime, selected.toImmutableList())
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(R.string.button_submit_dislike)) }
        },
        dismissButton = {
            TextButton(onClick = { openedAt = null }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}
