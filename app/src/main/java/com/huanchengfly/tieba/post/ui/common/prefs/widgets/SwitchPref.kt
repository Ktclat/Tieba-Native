package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import android.util.Log
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.huanchengfly.tieba.post.collectPreferenceAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.huanchengfly.tieba.post.ui.common.prefs.LocalPrefsDataStore
import androidx.compose.material3.Switch
import kotlinx.coroutines.launch

@Composable
fun SwitchPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: @Composable (value: Boolean) -> String? = { null },
    defaultChecked: Boolean = false,  // only used if it doesn't already exist in the datastore
    onCheckedChange: ((Boolean) -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val selectionKey = booleanPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val checked by datastore.collectPreferenceAsState(selectionKey, defaultChecked)

    fun edit(newState: Boolean) = run {
        onCheckedChange?.invoke(newState)
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = newState
                }
            } catch (e: Exception) {
                Log.e("SwitchPref", "Could not write pref $key to database. ${e.printStackTrace()}")
            }
        }
    }

    TextPref(
        title = title,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = { edit(it) },
        ),
        textColor = textColor,
        summary = summary(checked),
        darkenOnDisable = true,
        leadingIcon = leadingIcon,
        enabled = enabled,
    ) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = null,
        )
    }
}

/**
 * Simple preference with a trailing [Switch]
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultChecked If the switch should be checked by default. Only used if a value for this [key] doesn't already exist in the DataStore
 * @param onCheckedChange Will be called with the new state when the state changes
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be checked/unchecked
 * @param leadingIcon Icon which is positioned at the start of the Pref
 */
@Composable
fun SwitchPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    summaryOn: String? = null,
    summaryOff: String? = null,
    defaultChecked: Boolean = false,  // only used if it doesn't already exist in the datastore
    onCheckedChange: ((Boolean) -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {

    SwitchPref(
        key = key,
        title = title,
        modifier = modifier,
        summary = { checked -> (if (checked) summaryOn else summaryOff) ?: summary },
        defaultChecked = defaultChecked,
        onCheckedChange = onCheckedChange,
        textColor = textColor,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}
