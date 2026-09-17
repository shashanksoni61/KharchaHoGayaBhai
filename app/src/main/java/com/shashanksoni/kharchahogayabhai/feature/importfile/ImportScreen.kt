package com.shashanksoni.kharchahogayabhai.feature.importfile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashanksoni.kharchahogayabhai.R
import com.shashanksoni.kharchahogayabhai.core.common.DateTimeFormatters
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatch
import com.shashanksoni.kharchahogayabhai.domain.model.ImportBatchStatus
import com.shashanksoni.kharchahogayabhai.domain.model.TransactionSource
import com.shashanksoni.kharchahogayabhai.ui.util.labelRes
import java.time.ZoneId

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    zone: ZoneId,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.importFile(uri)
    }

    val requestSmsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.READ_SMS] == true) {
            viewModel.importSmsInbox()
        } else {
            viewModel.reportSmsPermissionDenied()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.import_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            openDocument.launch(
                                arrayOf(
                                    "text/*",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/csv",
                                    "application/vnd.ms-excel",
                                ),
                            )
                        },
                        enabled = !uiState.isImporting,
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null)
                        Text(
                            text = stringResource(R.string.import_csv),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            openDocument.launch(arrayOf("application/pdf"))
                        },
                        enabled = !uiState.isImporting,
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        Text(
                            text = stringResource(R.string.import_pdf),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_SMS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            viewModel.importSmsInbox()
                        } else {
                            requestSmsPermission.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS,
                                ),
                            )
                        }
                    },
                    enabled = !uiState.isImporting,
                ) {
                    Icon(Icons.Rounded.Sms, contentDescription = null)
                    Text(
                        text = stringResource(R.string.import_sms),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Text(
                    text = stringResource(R.string.import_sms_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (uiState.isImporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.import_in_progress))
                    }
                }

                uiState.lastResult?.let { result ->
                    ResultCard(batch = result.batch)
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.import_history_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (uiState.history.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.import_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            items(uiState.history, key = { it.id }) { batch ->
                HistoryRow(
                    batch = batch,
                    zone = zone,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ResultCard(batch: ImportBatch) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(batch.fileName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(
                    R.string.import_result_summary,
                    batch.totalParsed,
                    batch.newCount,
                    batch.mergedCount,
                    batch.duplicateCount,
                    batch.failedCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    batch: ImportBatch,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (batch.source) {
                    TransactionSource.PDF -> Icons.Rounded.PictureAsPdf
                    TransactionSource.SMS -> Icons.Rounded.Sms
                    else -> Icons.Rounded.UploadFile
                },
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(batch.fileName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(batch.source.labelRes) + " · " +
                        DateTimeFormatters.fullDate(batch.importedAt, zone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.import_history_counts,
                        batch.totalParsed,
                        batch.newCount,
                        batch.mergedCount,
                        batch.duplicateCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (batch.status != ImportBatchStatus.SUCCESS && batch.errorMessage != null) {
                    Text(
                        text = batch.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
