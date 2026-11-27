@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import com.huanchengfly.tieba.post.collectPreferenceAsState
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.prefs.LocalPrefsDataStore
import kotlinx.coroutines.launch

/**
 * Preference that shows a list of entries in a Dialog where multiple entries can be selected at one time.
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref. Shown above the summary and in the Dialog.
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultValue Default selected key if this Pref hasn't been saved already. Otherwise the value from the dataStore is used.
 * @param onValuesChange Will be called with the [Set] of selected keys when an item is selected/unselected
 * @param dialogBackgroundColor Background color of the Dialog
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be clicked and the Dialog cannot be shown.
 * @param entries Map of keys to values for entries that should be shown in the Dialog.
 */
@ExperimentalComposeUiApi
@Composable
fun MultiSelectListPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: Set<String> = setOf(),
    onValuesChange: ((Set<String>) -> Unit)? = null,
    dialogBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    entries: Map<String, String> = emptyMap(),
    leadingIcon: @Composable (() -> Unit)? = null,
    selectedSummary: (@Composable (Set<String>) -> String)? = null,
) {

    val entryList = remember(entries) { entries.toList() }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val selectionKey = stringSetPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val selected by datastore.collectPreferenceAsState(selectionKey, defaultValue)

    fun toggle(entryKey: String) {
        scope.launch {
            try {
                val saved = datastore.edit { preferences ->
                    val current = preferences[selectionKey] ?: defaultValue
                    preferences[selectionKey] =
                        if (entryKey in current) current - entryKey else current + entryKey
                }
                onValuesChange?.invoke(saved[selectionKey] ?: defaultValue)
            } catch (e: java.io.IOException) {
                Log.e("MultiSelectListPref", "Could not write preference $key", e)
            }
        }
    }

    TextPref(
        title = title,
        modifier = modifier,
        summary = selectedSummary?.invoke(selected) ?: summary,
        leadingIcon = leadingIcon,
        textColor = textColor,
        enabled = enabled,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                LazyColumn {
                    items(entryList, key = { it.first }) { (entryKey, label) ->
                        val checked = entryKey in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(checked, role = Role.Checkbox) { toggle(entryKey) }
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.button_sure_default))
                }
            },
            containerColor = dialogBackgroundColor,
        )
    }
}
