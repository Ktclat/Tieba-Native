@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.huanchengfly.tieba.post.collectPreferenceAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.prefs.LocalPrefsDataStore
import kotlinx.coroutines.launch

/**
 * Preference that shows a list of entries in a Dialog where a single entry can be selected at one time.
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref. Shown above the summary and in the Dialog.
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultValue Default selected key if this Pref hasn't been saved already. Otherwise the value from the dataStore is used.
 * @param onValueChange Will be called with the selected key when an item is selected
 * @param useSelectedAsSummary If true, uses the current selected item as the summary
 * @param dialogBackgroundColor Background color of the Dialog
 * @param contentColor Preferred content color passed to dialog's children
 * @param textColor Text colour of the [title], [summary] and [entries]
 * @param selectionColor Colour of the radiobutton of the selected item
 * @param buttonColor Colour of the cancel button
 * @param enabled If false, this Pref cannot be clicked and the Dialog cannot be shown.
 * @param entries Map of keys to values for entries that should be shown in the Dialog.
 */
@ExperimentalComposeUiApi
@Composable
fun ListPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    useSelectedAsSummary: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    entries: Map<String, String> = emptyMap(), //TODO: Change to List?
    icons: Map<String, @Composable () -> Unit> = emptyMap(),
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val selectionKey = stringPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val selected by datastore.collectPreferenceAsState(selectionKey, defaultValue)

    fun select(value: String) {
        showDialog = false
        onValueChange?.invoke(value)
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = value
                }
            } catch (e: Exception) {
                Log.e("ListPref", "Could not write pref $key to database. ${e.printStackTrace()}")
            }
        }
    }

    TextPref(
        title = title,
        summary = when {
            useSelectedAsSummary && selected != null -> entries[selected]
            useSelectedAsSummary && selected == null -> "Not Set"
            else -> summary
        },
        leadingIcon = leadingIcon,
        modifier = modifier,
        textColor = textColor,
        enabled = enabled,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val choices = remember(entries) { entries.toList() }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                LazyColumn(Modifier.selectableGroup()) {
                    items(choices, key = { it.first }) { (value, label) ->
                        val isSelected = value == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(isSelected, role = Role.RadioButton) { select(value) }
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            icons[value]?.invoke()
                            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            RadioButton(selected = isSelected, onClick = null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
        )
    }
}
