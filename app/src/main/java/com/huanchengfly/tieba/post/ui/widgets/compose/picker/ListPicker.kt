package com.huanchengfly.tieba.post.ui.widgets.compose.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun <ItemValue> ListSinglePicker(
    itemTitles: ImmutableList<String>,
    itemValues: ImmutableList<ItemValue>,
    selectedPosition: Int,
    onItemSelected: (position: Int, title: String, value: ItemValue, changed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    itemIcons: ImmutableMap<ItemValue, @Composable () -> Unit> = persistentMapOf(),
    selectedIndicator: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null
        )
    },
    colors: PickerColors = PickerDefaults.pickerColors(),
    enabled: Boolean = true,
) {
    require(itemTitles.size == itemValues.size)
    Column(modifier.selectableGroup()) {
        itemTitles.forEachIndexed { index, title ->
            val selected = index == selectedPosition
            val label = if (selected) colors.selectedItemColor(enabled).value else colors.itemColor(enabled).value
            ListItem(
                headlineContent = { Text(title) },
                leadingContent = itemIcons[itemValues[index]],
                trailingContent = if (selected) selectedIndicator else null,
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = label,
                    leadingIconColor = label,
                    trailingIconColor = label,
                ),
                modifier = Modifier.selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { onItemSelected(index, title, itemValues[index], !selected) },
                ),
            )
        }
    }
}
