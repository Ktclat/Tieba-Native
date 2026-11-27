@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.huanchengfly.tieba.post.ui.page.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString

@Composable
internal fun UserProfileDetail(
    user: ImmutableHolder<User>,
    modifier: Modifier = Modifier,
    showBtn: Boolean = true,
    isSelf: Boolean = false,
    actionEnabled: Boolean = true,
    onBtnClick: () -> Unit = {},
    onCopyIdClick: (String) -> Unit = {},
) {
    val info = user.get()
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val id = info.tieba_uid.trim().takeIf { it.isNotEmpty() && it != "0" }
        ?: info.id.takeIf { it > 0 }?.toString()
    val verification = info.bazhu_grade?.desc?.takeIf { it.isNotBlank() } ?: info.new_god_data
        ?.takeIf { it.status != 0 && it.field_name.isNotBlank() }
        ?.let { stringResource(R.string.text_god_verify, it.field_name) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        // A narrow tablet sidebar may have less room than a phone.
        val inlineAction = maxWidth >= 308.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    data = StringUtil.getBigAvatarUrl(info.portraith.ifBlank { info.portrait }),
                    size = 64.dp,
                    contentDescription = null,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = StringUtil.getUsernameAnnotatedString(
                            LocalContext.current, info.name, info.nameShow.ifBlank { info.name }, colors.onSurfaceVariant,
                        ),
                        modifier = Modifier.semantics { heading() },
                        style = typography.titleLarge,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (id != null) {
                        val copyLabel = stringResource(R.string.title_copy)
                        TextButton(
                            onClick = { onCopyIdClick(id) },
                            modifier = Modifier.semantics { onClick(label = copyLabel, action = null) },
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant),
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.text_profile_user_id, id),
                                    style = typography.bodySmall,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Icon(
                                    ImageVector.vectorResource(R.drawable.ms_content_copy),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
                if (showBtn && inlineAction) ProfileAction(isSelf, info.has_concerned != 0, actionEnabled, onBtnClick)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ProfileStat(info.concern_num.getShortNumString(), stringResource(R.string.text_stat_follow))
                ProfileStat(info.fans_num.getShortNumString(), stringResource(R.string.text_stat_fans))
                ProfileStat(info.total_agree_num.getShortNumString(), stringResource(R.string.text_stat_agrees))
            }
            if (info.intro.isNotBlank()) {
                Text(
                    text = info.intro, style = typography.bodyMedium, color = colors.onSurface,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
            if (verification != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ms_verified), contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = colors.primary,
                    )
                    Text(verification, style = typography.labelMedium, color = colors.primary)
                }
            }
            val sex = when (info.sex) {
                1 -> stringResource(R.string.profile_sex_male)
                2 -> stringResource(R.string.profile_sex_female)
                else -> null
            }
            val details = listOfNotNull(
                sex,
                info.ip_address.takeIf { it.isNotBlank() }?.let { stringResource(R.string.text_profile_ip_location, it) },
                info.tb_age.takeIf { it.isNotBlank() }?.let { stringResource(R.string.text_profile_tb_age, it) },
            )
            if (details.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    details.forEach { Text(it, style = typography.bodySmall, color = colors.onSurfaceVariant) }
                }
            }
            if (showBtn && !inlineAction) ProfileAction(isSelf, info.has_concerned != 0, actionEnabled, onBtnClick)
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileAction(isSelf: Boolean, following: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (following && !isSelf) {
        FilledTonalButton(onClick = onClick, enabled = enabled, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.button_unfollow))
        }
    } else {
        Button(onClick = onClick, enabled = enabled, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(if (isSelf) R.string.menu_edit_info else R.string.button_follow))
        }
    }
}
