package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.GoogleUserProfile
import com.example.ui.theme.GlassTheme
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSheet(
    isAutoSync: Boolean,
    readerMode: String = "html",
    syncFolderName: String?,
    lastSyncTime: Long,
    googleUser: GoogleUserProfile?,
    isGoogleSyncing: Boolean = false,
    googleSyncStatus: String? = null,
    lastGoogleDriveBackupTime: Long = 0L,
    googleDriveAutoBackup: Boolean = true,
    deleteFromStorageWhenDeleted: Boolean = true,
    onDismiss: () -> Unit,
    onSelectFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onSyncNow: () -> Unit,
    onSyncFullDevice: () -> Unit,
    onSyncPdfDevice: () -> Unit,
    onImportFiles: () -> Unit,
    onImportPdf: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSignInGoogle: () -> Unit,
    onChooseDeviceAccount: () -> Unit = onSignInGoogle,
    onSignOutGoogle: () -> Unit,
    onBackupToDrive: () -> Unit,
    onRestoreFromDrive: () -> Unit,
    onToggleGoogleDriveAutoBackup: (Boolean) -> Unit,
    onToggleDeleteFromStorageWhenDeleted: (Boolean) -> Unit
) {
    val colors = GlassTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isPdfMode = readerMode == "pdf"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        scrimColor = colors.shadow.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Sync & Cloud Backup",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = colors.text,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            // ==========================================
            // SECTION 1: GOOGLE DRIVE CLOUD INTEGRATION
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.field)
                    .border(
                        1.dp,
                        if (googleUser != null) Color(0xFF4285F4).copy(alpha = 0.35f) else colors.hairline,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Google Drive",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Drive Cloud Backup",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = if (googleUser != null) "Connected · Auto-sync enabled" else "Not connected",
                            fontSize = 12.sp,
                            color = if (googleUser != null) Color(0xFF34C759) else colors.textTertiary
                        )
                    }

                    if (isGoogleSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF4285F4)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (googleUser == null) {
                    // Not signed in state
                    Text(
                        text = "Connect your personal Google Account to back up and restore your notes, documents, and books to your own Google Drive. Backups are stored exclusively in the authenticated user's account.",
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Sign-in button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4285F4))
                            .clickable { onSignInGoogle() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in with Google Account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary option: Device Account Picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
                            .clickable { onChooseDeviceAccount() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = colors.text,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Choose Device Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text
                            )
                        }
                    }
                } else {
                    // Signed in user profile view
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.card)
                            .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = googleUser.displayName.take(1).uppercase(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = googleUser.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = googleUser.email,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account management actions: Switch Account & Sign Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.field)
                                    .clickable { onChooseDeviceAccount() }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Switch Account",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4285F4)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.field)
                                    .clickable { onSignOutGoogle() }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign out",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (lastGoogleDriveBackupTime > 0L) {
                        Text(
                            text = "Last Google Drive backup: ${DateFormatter.fmtClock(lastGoogleDriveBackupTime)}",
                            fontSize = 11.5.sp,
                            color = colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (googleSyncStatus != null) {
                        Text(
                            text = googleSyncStatus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Action buttons: Backup & Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4285F4))
                                .clickable { onBackupToDrive() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Back Up Now",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card)
                                .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
                                .clickable { onRestoreFromDrive() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = colors.text,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Restore Notes",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-sync to Drive switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-sync with Google Drive",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text
                            )
                            Text(
                                text = "Seamlessly saves backup to Drive on changes",
                                fontSize = 11.5.sp,
                                color = colors.textTertiary
                            )
                        }
                        Switch(
                            checked = googleDriveAutoBackup,
                            onCheckedChange = onToggleGoogleDriveAutoBackup,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4285F4)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // SECTION 2: DEVICE STORAGE & LOCAL SYNC
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.field)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPdfMode) Color(0xFFDC2626) else Color(0xFF34C759))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPdfMode) "Full Device PDF Sync" else "Full Device HTML Sync",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.chipOnBg)
                            .clickable {
                                onDismiss()
                                if (isPdfMode) onSyncPdfDevice() else onSyncFullDevice()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Scan All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.chipOnTx
                        )
                    }
                }

                Text(
                    text = if (isPdfMode) {
                        "Automatically indexes and discovers all PDF documents across your phone (Documents, Downloads, Books, storage, and MediaStore) with zero quality loss."
                    } else {
                        "Automatically indexes and syncs every .html and .htm file found across your phone (Documents, Downloads, storage, and MediaStore)."
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (lastSyncTime > 0L) {
                    Text(
                        text = "Last device scan: ${DateFormatter.fmtClock(lastSyncTime)}",
                        fontSize = 11.5.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Full Device PDF Scan
            SyncActionRow(
                icon = Icons.Default.PictureAsPdf,
                label = "Scan device for PDF Documents",
                sub = "Search phone storage for all .pdf books and files",
                onClick = {
                    onDismiss()
                    onSyncPdfDevice()
                }
            )

            // Action: Full Device HTML Scan
            SyncActionRow(
                icon = Icons.Default.Html,
                label = "Scan device for HTML Notes",
                sub = "Search phone storage for .html & .htm notes",
                onClick = {
                    onDismiss()
                    onSyncFullDevice()
                }
            )

            // Current Sync Folder Option
            val hasFolder = !syncFolderName.isNullOrEmpty()
            SyncActionRow(
                icon = if (hasFolder) Icons.Default.FolderOpen else Icons.Default.CreateNewFolder,
                label = if (hasFolder) "Synced folder: $syncFolderName" else "Select custom sync folder",
                sub = if (hasFolder) "Tap to change folder" else "Limit auto-sync to a specific directory",
                onClick = { onDismiss(); onSelectFolder() }
            )

            if (hasFolder) {
                SyncActionRow(
                    icon = Icons.Default.Sync,
                    label = "Sync selected folder now",
                    sub = "Scan $syncFolderName and update notes",
                    onClick = {
                        onDismiss()
                        onSyncNow()
                    }
                )
            }

            // Action: Import individual PDF files
            SyncActionRow(
                icon = Icons.Default.PictureAsPdf,
                label = "Import PDF Document",
                sub = "Select and import a PDF file to read with dynamic zoom",
                onClick = { onDismiss(); onImportPdf() }
            )

            // Action: Import individual HTML / Text files
            SyncActionRow(
                icon = Icons.Default.UploadFile,
                label = "Import HTML / Text files",
                sub = "Pick specific .html, .md, or .txt documents",
                onClick = { onDismiss(); onImportFiles() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // SECTION 3: SYSTEM DELETION & DEDUPLICATION SETTINGS
            // ==========================================
            // Delete from device when deleted in app switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.field)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delete file from device storage",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Deleting a synced note in the app also removes the physical file from phone storage",
                        fontSize = 11.5.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Switch(
                    checked = deleteFromStorageWhenDeleted,
                    onCheckedChange = onToggleDeleteFromStorageWhenDeleted,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF3B30)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-sync switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.field)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-scan storage on app launch",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Keeps device notes and documents synchronized",
                        fontSize = 11.5.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Switch(
                    checked = isAutoSync,
                    onCheckedChange = onToggleAutoSync,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deduplication badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF34C759).copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Deduplication: Duplicate notes are automatically prevented and cleaned across Google Drive and local storage.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Done button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.field)
                    .clickable { onDismiss() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SyncActionRow(
    icon: ImageVector,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    val colors = GlassTheme.colors
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.field),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text
            )
            Text(
                text = sub,
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
