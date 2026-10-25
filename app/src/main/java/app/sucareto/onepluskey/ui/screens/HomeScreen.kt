package app.sucareto.onepluskey.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sucareto.onepluskey.R
import app.sucareto.onepluskey.config.Gesture
import app.sucareto.onepluskey.config.KeyAction
import app.sucareto.onepluskey.ui.ActionKind
import app.sucareto.onepluskey.ui.colorOsFunctionLabel
import app.sucareto.onepluskey.ui.UiState
import app.sucareto.onepluskey.ui.components.ListCard
import app.sucareto.onepluskey.ui.components.SectionHeader
import app.sucareto.onepluskey.ui.components.SettingRow
import app.sucareto.onepluskey.ui.components.gestureLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onToggleEnabled: (Boolean) -> Unit,
    onEditGesture: (Gesture) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusCard(state)

            if (state.configurable) {
                SettingRow(
                    title = stringResource(R.string.master_enable),
                    subtitle = stringResource(R.string.master_enable_desc),
                ) {
                    Switch(checked = state.enabled, onCheckedChange = onToggleEnabled)
                }

                SectionHeader(stringResource(R.string.section_gestures))
                Gesture.entries.forEach { gesture ->
                    val binding = state.binding(gesture)
                    val kind = ActionKind.of(binding.action)
                    val action = binding.action
                    val subtitle = if (action is KeyAction.ColorOsFunction) {
                        stringResource(colorOsFunctionLabel(action.function))
                    } else {
                        stringResource(kind.labelRes)
                    }
                    ListCard(
                        title = stringResource(gestureLabel(gesture)),
                        subtitle = subtitle,
                        onClick = { onEditGesture(gesture) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(state: UiState) {
    val active = state.moduleActive
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(if (active) R.drawable.ic_check_circle else R.drawable.ic_error),
                contentDescription = null,
            )
            Column {
                Text(
                    text = stringResource(if (active) R.string.module_active else R.string.module_inactive),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (active) {
                        stringResource(R.string.module_active_desc, state.frameworkName ?: "-")
                    } else {
                        stringResource(R.string.module_inactive_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
