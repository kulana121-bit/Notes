package com.example.ui.sheets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.example.data.api.GeminiClient
import com.example.data.model.AppSettings
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.LocalThemeTransition
import com.example.util.VibrationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    totalNotesCount: Int,
    approxStorageKb: Int,
    onDismiss: () -> Unit,
    onSetTheme: (String) -> Unit,
    onSetReduceTransparency: (Boolean) -> Unit,
    onSetReadingFontSize: (Int) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit = {},
    onSetPdfPageMode: (String) -> Unit = {},
    onSetPdfColorFilter: (String) -> Unit = {},
    onSetPdfRenderQuality: (String) -> Unit = {},
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onRemoveDuplicates: () -> Unit,
    onWipeAllNotes: () -> Unit,
    onSetDeleteFromStorageWhenDeleted: (Boolean) -> Unit = {},
    onSetGeminiApiKey: (String) -> Unit = {}
) {
    val colors = GlassTheme.colors
    var deleteArmed by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyDraft by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                text = "Settings",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = colors.text,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            // Section: Appearance
            SectionHeader(title = "Appearance")

            // Theme selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.DarkMode)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Theme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                // Theme Segment
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.field)
                        .padding(2.dp)
                ) {
                    ThemeSegmentButton("Auto", settings.theme == "auto") { onSetTheme("auto") }
                    ThemeSegmentButton("Light", settings.theme == "light") { onSetTheme("light") }
                    ThemeSegmentButton("Dark", settings.theme == "dark") { onSetTheme("dark") }
                }
            }

            // Transparent Liquid Glass Effect
            val localContext = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.BlurOn)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transparent Liquid Glass Effect",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Frosted glass translucency, glossy reflections, and ambient depth",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = !settings.reduceTransparency,
                    onCheckedChange = { isLiquidGlassOn ->
                        VibrationHelper.click(localContext)
                        onSetReduceTransparency(!isLiquidGlassOn)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759)
                    )
                )
            }

            // Realistic Haptic Feedback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.Vibration)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Realistic Haptic Feedback",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Tactile vibration responses on taps, gestures, and note actions",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { isEnabled ->
                        onSetHapticsEnabled(isEnabled)
                        if (isEnabled) {
                            VibrationHelper.click(localContext)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759)
                    )
                )
            }

            // Reading font size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.TextFields)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "HTML & Text Font Size",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.field)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { onSetReadingFontSize(settings.readingFontSize - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    }
                    Text(
                        text = "${settings.readingFontSize}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.text,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { onSetReadingFontSize(settings.readingFontSize + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    }
                }
            }

            // Section: PDF Reader Engine & Quality
            SectionHeader(title = "PDF Reader Engine")

            // PDF Quality & Dynamic Zoom Sharpness
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.HighQuality)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Zoom Sharpness",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Auto-enhances text clarity at higher zoom levels",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.field)
                        .padding(2.dp)
                ) {
                    ThemeSegmentButton("Sharp", settings.pdfRenderQuality == "sharp") { onSetPdfRenderQuality("sharp") }
                    ThemeSegmentButton("Eco", settings.pdfRenderQuality == "eco") { onSetPdfRenderQuality("eco") }
                }
            }

            // PDF Page Flow Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.ViewCarousel)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Default PDF Layout",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Continuous vertical scroll or single-page swipe",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.field)
                        .padding(2.dp)
                ) {
                    ThemeSegmentButton("Vertical", settings.pdfPageMode == "vertical") { onSetPdfPageMode("vertical") }
                    ThemeSegmentButton("Pager", settings.pdfPageMode == "horizontal") { onSetPdfPageMode("horizontal") }
                }
            }

            // Section: AI Intelligence
            SectionHeader(title = "AI Assistant")

            val isKeyConnected = GeminiClient.isValidGeminiApiKey(settings.geminiApiKey) || GeminiClient.isValidGeminiApiKey(GeminiClient.getApiKey())
            val context = androidx.compose.ui.platform.LocalContext.current
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        apiKeyDraft = settings.geminiApiKey
                        showApiKeyDialog = true
                    }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.AutoAwesome)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Gemini API Key",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isKeyConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isKeyConnected) "Connected" else "Not Connected",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isKeyConnected) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }
                    Text(
                        text = if (settings.geminiApiKey.isNotBlank()) "Custom key configured: ${settings.geminiApiKey.take(7)}••••" else "Tap to connect your free Gemini API key",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Edit API Key",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (showApiKeyDialog) {
                AlertDialog(
                    onDismissRequest = { showApiKeyDialog = false },
                    title = {
                        Text(
                            text = "Gemini AI API Key",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Connect your personal Google Gemini API key to activate cloud AI note synthesis, code generation, and intelligent search.",
                                fontSize = 13.5.sp,
                                color = colors.textSecondary,
                                lineHeight = 19.sp
                            )
                            OutlinedTextField(
                                value = apiKeyDraft,
                                onValueChange = { apiKeyDraft = it },
                                label = { Text("API Key (starts with AIzaSy...)") },
                                placeholder = { Text("Paste your Gemini API key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val clip = clipboard.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            apiKeyDraft = clip.trim()
                                        }
                                    }
                                ) {
                                    Text("Paste")
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://aistudio.google.com/app/apikey")
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Text("Get Free Key ↗")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onSetGeminiApiKey(apiKeyDraft.trim())
                                showApiKeyDialog = false
                            }
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        if (settings.geminiApiKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    onSetGeminiApiKey("")
                                    apiKeyDraft = ""
                                    showApiKeyDialog = false
                                }
                            ) {
                                Text("Clear Key", color = Color(0xFFEF4444))
                            }
                        } else {
                            TextButton(onClick = { showApiKeyDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }

            // Section: Library & Backup
            SectionHeader(title = "Library")

            SettingsActionRow(
                icon = Icons.Default.Download,
                label = "Export backup",
                sub = "Download every note as one JSON file",
                onClick = { onDismiss(); onExportBackup() }
            )

            SettingsActionRow(
                icon = Icons.Default.Upload,
                label = "Restore backup",
                sub = "Duplicates are skipped automatically",
                onClick = { onDismiss(); onRestoreBackup() }
            )

            SettingsActionRow(
                icon = Icons.Default.Delete,
                label = "Remove duplicate notes",
                sub = "Scans library and removes identical copies",
                onClick = { onDismiss(); onRemoveDuplicates() }
            )

            // Also delete original file from phone storage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.Delete)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Also delete original file from phone storage",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Deletes source PDF/HTML files from device storage when deleted in app",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = settings.deleteFromStorageWhenDeleted,
                    onCheckedChange = onSetDeleteFromStorageWhenDeleted,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759)
                    )
                )
            }

            // Storage row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.Info)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Storage",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "$totalNotesCount notes & documents · $approxStorageKb KB used",
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            // Section: Danger zone
            SectionHeader(title = "Danger zone")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.danger.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Delete all notes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.danger,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.field)
                        .clickable {
                            if (!deleteArmed) {
                                deleteArmed = true
                            } else {
                                onWipeAllNotes()
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (deleteArmed) "Tap again" else "Delete",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.danger
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
private fun SectionHeader(title: String) {
    val colors = GlassTheme.colors
    Text(
        text = title.uppercase(),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        color = colors.textTertiary,
        modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    val colors = GlassTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.field),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ThemeSegmentButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = GlassTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeTransition = LocalThemeTransition.current
    var buttonCenter by remember { mutableStateOf(Offset.Unspecified) }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val rootPos = coords.positionInRoot()
                val sz = coords.size
                buttonCenter = Offset(rootPos.x + sz.width / 2f, rootPos.y + sz.height / 2f)
            }
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) colors.card else Color.Transparent)
            .clickable {
                VibrationHelper.click(context)
                if (buttonCenter != Offset.Unspecified) {
                    themeTransition.recordOrigin(buttonCenter)
                }
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.text else colors.textTertiary
        )
    }
}

@Composable
private fun SettingsActionRow(
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text
            )
            Text(
                text = sub,
                fontSize = 12.5.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
