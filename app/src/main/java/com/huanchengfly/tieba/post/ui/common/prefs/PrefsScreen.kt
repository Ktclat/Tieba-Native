package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.huanchengfly.tieba.post.ui.widgets.compose.Container

val LocalPrefsDataStore: ProvidableCompositionLocal<DataStore<Preferences>> =
    staticCompositionLocalOf { error("PrefsScreen must provide a DataStore") }

/**
 * Shared card-based preference layout for every settings sub-page.
 * Headers and group spacers are not drawn inside cards. Each actionable row
 * is an independent accessible target and retains its original DataStore key.
 */
@Composable
fun PrefsScreen(
    dataStore: DataStore<Preferences>,
    modifier: Modifier = Modifier,
    dividerThickness: Dp = 0.dp,
    dividerIndent: Dp = 0.dp,
    content: PrefsScope.() -> Unit
) {
    val prefsScope = PrefsScopeImpl().apply(content)

    CompositionLocalProvider(LocalPrefsDataStore provides dataStore) {
        Container {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(prefsScope.prefsItems.size, key = { "setting-$it" }) { index ->
                    val groupHeader = prefsScope.headerIndexes.contains(index)
                    val groupSpacer = prefsScope.footerIndexes.contains(index) &&
                        prefsScope.footerIndexes.contains(index - 1)
                    when {
                        groupHeader -> prefsScope.getPrefsItem(index)()
                        groupSpacer -> Spacer(Modifier.height(10.dp))
                        else -> Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = run {
                                val outer = Material3Theme.shapes.large.topStart
                                val inner = Material3Theme.shapes.extraSmall.topStart
                                val first = index == 0 || index - 1 in prefsScope.headerIndexes || index - 1 in prefsScope.footerIndexes
                                val last = index == prefsScope.prefsItems.lastIndex || index in prefsScope.footerIndexes || index + 1 in prefsScope.headerIndexes
                                val top = if (first) outer else inner
                                val bottom = if (last) outer else inner
                                RoundedCornerShape(top, top, bottom, bottom)
                            },
                            color = Material3Theme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                        ) {
                            Column {
                                prefsScope.getPrefsItem(index)()
                                if (dividerThickness > 0.dp &&
                                    index < prefsScope.prefsItems.lastIndex &&
                                    !prefsScope.headerIndexes.contains(index + 1) &&
                                    !prefsScope.footerIndexes.contains(index)
                                ) {
                                    Divider(
                                        thickness = dividerThickness,
                                        indent = dividerIndent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
