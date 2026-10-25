package app.sucareto.onepluskey.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sucareto.onepluskey.R
import app.sucareto.onepluskey.config.Binding
import app.sucareto.onepluskey.config.ColorOsFunctions
import app.sucareto.onepluskey.config.ConfigKeys
import app.sucareto.onepluskey.config.Gesture
import app.sucareto.onepluskey.config.IntentTarget
import app.sucareto.onepluskey.config.KeyAction
import app.sucareto.onepluskey.config.ScreenOffMode
import app.sucareto.onepluskey.ui.ActionKind
import app.sucareto.onepluskey.ui.colorOsFunctionLabel
import app.sucareto.onepluskey.ui.components.AppPickerDialog
import app.sucareto.onepluskey.ui.components.Dropdown
import app.sucareto.onepluskey.ui.components.ListCard
import app.sucareto.onepluskey.ui.components.SettingRow
import app.sucareto.onepluskey.ui.components.SliderRow
import app.sucareto.onepluskey.ui.components.gestureLabel

@Composable
fun GestureEditorScreen(
    gesture: Gesture,
    saved: Binding,
    savedTimingMs: Int?,
    onSave: (Binding, Int?) -> Unit,
    onBack: () -> Unit,
) {
    var draft by rememberSaveable(gesture, stateSaver = bindingSaver(gesture)) { mutableStateOf(saved) }
    var timingMs by rememberSaveable(gesture) { mutableStateOf(savedTimingMs) }
    BackHandler(onBack = onBack)
    EditorContent(
        gesture = gesture,
        binding = draft,
        timingMs = timingMs,
        dirty = draft != saved || timingMs != savedTimingMs,
        onChange = { draft = it },
        onTimingChange = { timingMs = it },
        onSave = { onSave(draft, timingMs) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorContent(
    gesture: Gesture,
    binding: Binding,
    timingMs: Int?,
    dirty: Boolean,
    onChange: (Binding) -> Unit,
    onTimingChange: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    var showAppPicker by remember { mutableStateOf(false) }
    val kinds = remember(gesture) {
        ActionKind.entries.filter { it != ActionKind.NATIVE || gesture.supportsNative }
    }
    val kind = ActionKind.of(binding.action)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(gestureLabel(gesture))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = dirty) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Dropdown(
                label = stringResource(R.string.label_action),
                options = kinds,
                selected = kind,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { onChange(binding.copy(action = ActionKind.blank(it))) },
            )

            ActionFields(
                action = binding.action,
                onChange = { onChange(binding.copy(action = it)) },
                onPickApp = { showAppPicker = true },
            )

            val systemHaptics = kind == ActionKind.COLOROS ||
                (kind == ActionKind.NATIVE && gesture == Gesture.LONG)
            if (!systemHaptics) {
                SettingRow(title = stringResource(R.string.label_vibrate)) {
                    Switch(
                        checked = binding.vibrate,
                        onCheckedChange = { onChange(binding.copy(vibrate = it)) },
                    )
                }
            }

            Dropdown(
                label = stringResource(R.string.label_screen_off),
                options = ScreenOffMode.entries.toList(),
                selected = binding.screenOff,
                optionLabel = { stringResource(screenOffLabel(it)) },
                onSelect = { onChange(binding.copy(screenOff = it)) },
            )

            if (timingMs != null) {
                val (titleRes, range) = when (gesture) {
                    Gesture.LONG -> R.string.setting_long_press to ConfigKeys.LONG_PRESS_RANGE
                    else -> R.string.setting_multi_tap to ConfigKeys.MULTI_TAP_RANGE
                }
                SliderRow(
                    title = stringResource(titleRes),
                    valueMs = timingMs,
                    range = range,
                    onChange = onTimingChange,
                )
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onPick = { app ->
                val updated = when (val action = binding.action) {
                    is KeyAction.LaunchApp -> action.copy(packageName = app.packageName)
                    is KeyAction.StartActivity -> action.copy(packageName = app.packageName)
                    is KeyAction.SendIntent -> action.copy(packageName = app.packageName)
                    else -> action
                }
                onChange(binding.copy(action = updated))
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

private fun bindingSaver(gesture: Gesture): Saver<Binding, String> =
    Saver(save = { it.encode() }, restore = { Binding.decode(it, gesture) })

@Composable
private fun ActionFields(action: KeyAction, onChange: (KeyAction) -> Unit, onPickApp: () -> Unit) {
    when (action) {
        KeyAction.Native -> Hint(R.string.native_hint)
        KeyAction.Assistant -> {
            val context = LocalContext.current
            ListCard(
                title = stringResource(R.string.assistant_settings),
                subtitle = stringResource(R.string.assistant_settings_desc),
                onClick = { openAssistantSettings(context) },
            )
        }
        is KeyAction.ColorOsFunction -> {
            Dropdown(
                label = stringResource(R.string.label_coloros_function),
                options = ColorOsFunctions.known,
                selected = action.function,
                optionLabel = { stringResource(colorOsFunctionLabel(it)) },
                onSelect = { onChange(action.copy(function = it)) },
            )
            Hint(R.string.coloros_hint)
        }
        is KeyAction.LaunchApp -> PackageField(action.packageName, onPickApp)
        is KeyAction.StartActivity -> {
            PackageField(action.packageName, onPickApp)
            Field(R.string.label_class, action.className) { onChange(action.copy(className = it)) }
        }
        is KeyAction.SendIntent -> {
            Dropdown(
                label = stringResource(R.string.label_intent_target),
                options = IntentTarget.entries.toList(),
                selected = action.target,
                optionLabel = { stringResource(intentTargetLabel(it)) },
                onSelect = { onChange(action.copy(target = it)) },
            )
            Field(R.string.label_intent_action, action.action) { onChange(action.copy(action = it)) }
            Field(R.string.label_intent_data, action.data) { onChange(action.copy(data = it)) }
            Field(R.string.label_package, action.packageName) { onChange(action.copy(packageName = it)) }
            Field(R.string.label_class, action.className) { onChange(action.copy(className = it)) }
            Field(R.string.label_intent_extras, action.extras) { onChange(action.copy(extras = it)) }
        }
        is KeyAction.Shell -> {
            Field(R.string.label_command, action.command, singleLine = false) {
                onChange(action.copy(command = it))
            }
            SettingRow(title = stringResource(R.string.label_run_as_root)) {
                Switch(checked = action.root, onCheckedChange = { onChange(action.copy(root = it)) })
            }
        }
        else -> Unit
    }
}

@Composable
private fun Hint(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun PackageField(value: String, onPickApp: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.label_app)) },
        trailingIcon = {
            TextButton(onClick = onPickApp) {
                Text(stringResource(R.string.pick_app))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Field(labelRes: Int, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun openAssistantSettings(context: Context) {
    val candidates = listOf(Settings.ACTION_VOICE_INPUT_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    for (action in candidates) {
        try {
            context.startActivity(Intent(action))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
}

private fun screenOffLabel(mode: ScreenOffMode): Int = when (mode) {
    ScreenOffMode.IGNORE -> R.string.screen_off_ignore
    ScreenOffMode.WAKE -> R.string.screen_off_wake
    ScreenOffMode.SILENT -> R.string.screen_off_silent
}

private fun intentTargetLabel(target: IntentTarget): Int = when (target) {
    IntentTarget.ACTIVITY -> R.string.target_activity
    IntentTarget.BROADCAST -> R.string.target_broadcast
    IntentTarget.SERVICE -> R.string.target_service
    IntentTarget.FOREGROUND_SERVICE -> R.string.target_foreground_service
}
