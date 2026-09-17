package com.shashanksoni.kharchahogayabhai.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionLabel
import com.shashanksoni.kharchahogayabhai.ui.component.SectionCard
import com.shashanksoni.kharchahogayabhai.ui.util.CategoryVisuals
import java.time.ZoneId

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenImport: () -> Unit,
    zone: ZoneId,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showCreateLabelDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<SmsPermissionAction?>(null) }

    val requestSmsPermissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val action = pendingAction
        pendingAction = null
        val allowed = grants[Manifest.permission.READ_SMS] == true
        if (allowed && action != null) {
            when (action) {
                SmsPermissionAction.ManualScan -> viewModel.scanNewSms()
                SmsPermissionAction.FullRescan -> viewModel.rescanAllSms()
                SmsPermissionAction.EnableBackground ->
                    viewModel.setListenSmsInBackground(true)
            }
        } else if (action != null) {
            viewModel.reportSmsPermissionDenied()
            if (action == SmsPermissionAction.EnableBackground) {
                viewModel.setListenSmsInBackground(false)
            }
        }
    }

    fun ensureSmsPermission(action: SmsPermissionAction) {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (readGranted && (action != SmsPermissionAction.EnableBackground || receiveGranted)) {
            when (action) {
                SmsPermissionAction.ManualScan -> viewModel.scanNewSms()
                SmsPermissionAction.FullRescan -> viewModel.rescanAllSms()
                SmsPermissionAction.EnableBackground ->
                    viewModel.setListenSmsInBackground(true)
            }
            return
        }
        pendingAction = action
        requestSmsPermissions.launch(
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = stringResource(R.string.settings_sms_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_sms_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.lastSmsScanAt?.let { instant ->
                        stringResource(
                            R.string.settings_sms_last_scan,
                            DateTimeFormatters.fullDateTime(instant, zone),
                        )
                    } ?: stringResource(R.string.settings_sms_never_scanned),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { ensureSmsPermission(SmsPermissionAction.ManualScan) },
                    enabled = !uiState.isScanningSms && !uiState.isResetting,
                ) {
                    if (uiState.isScanningSms) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Sms, contentDescription = null)
                    }
                    Text(
                        text = stringResource(R.string.settings_sms_scan_new),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TextButton(
                    onClick = { ensureSmsPermission(SmsPermissionAction.FullRescan) },
                    enabled = !uiState.isScanningSms && !uiState.isResetting,
                ) {
                    Text(stringResource(R.string.settings_sms_rescan_all))
                }
                SettingSwitchRow(
                    title = stringResource(R.string.settings_sms_listen_title),
                    explanation = stringResource(R.string.settings_sms_listen_explanation),
                    checked = uiState.listenSmsInBackground,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            ensureSmsPermission(SmsPermissionAction.EnableBackground)
                        } else {
                            viewModel.setListenSmsInBackground(false)
                        }
                    },
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_sms_auto_title),
                    explanation = stringResource(R.string.settings_sms_auto_explanation),
                    checked = uiState.autoSmsScanOnOpen,
                    onCheckedChange = viewModel::setAutoSmsScanOnOpen,
                )
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_import_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_import_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onOpenImport) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Text(
                        text = stringResource(R.string.settings_open_import),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_data_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_reset_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { showResetDialog = true },
                    enabled = !uiState.isResetting && !uiState.isScanningSms,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    if (uiState.isResetting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(stringResource(R.string.settings_reset_button))
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.settings_labels_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                trailing = {
                    TextButton(onClick = { showCreateLabelDialog = true }) {
                        Text(stringResource(R.string.settings_add_label))
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.settings_labels_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(uiState.labels, key = { it.id }) { label ->
            LabelSettingsRow(
                label = label,
                onDelete = { viewModel.deleteCustomLabel(label.id) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        uiState.errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        uiState.infoMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (showResetDialog) {
        ResetDataWarningDialog(
            onConfirm = {
                showResetDialog = false
                viewModel.resetAllData()
            },
            onDismiss = { showResetDialog = false },
        )
    }

    if (showCreateLabelDialog) {
        CreateLabelDialog(
            onConfirm = { name ->
                showCreateLabelDialog = false
                viewModel.createCustomLabel(name)
            },
            onDismiss = { showCreateLabelDialog = false },
        )
    }
}

private enum class SmsPermissionAction {
    ManualScan,
    FullRescan,
    EnableBackground,
}

@Composable
private fun SettingSwitchRow(
    title: String,
    explanation: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LabelSettingsRow(
    label: TransactionLabel,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label.name,
            style = MaterialTheme.typography.bodyLarge,
            color = CategoryVisuals.colorOf(label.colorHex, MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (label.isSystemDefined) {
                stringResource(R.string.settings_label_builtin)
            } else {
                stringResource(R.string.settings_label_custom)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!label.isSystemDefined) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.settings_delete_label),
                )
            }
        }
    }
}

@Composable
private fun ResetDataWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_reset_dialog_body))
                Text(
                    text = stringResource(R.string.settings_reset_dialog_kept),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.settings_reset_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_reset_cancel))
            }
        },
    )
}

@Composable
private fun CreateLabelDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_add_label_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_label_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.settings_add_label_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_dismiss))
            }
        },
    )
}
