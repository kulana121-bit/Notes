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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import com.example.data.api.GeminiClient
import com.example.data.model.AppSettings
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.LocalThemeTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.AccentPalettes
import com.example.util.DateFormatter
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
    onSetCustomAppName: (String) -> Unit = {},
    onSetAccentPalette: (String) -> Unit = {},
    onSetAppIconPreset: (String) -> Unit = {},
    onSetFontFamilyStyle: (String) -> Unit = {},
    onResetCustomization: () -> Unit = {},
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onRemoveDuplicates: () -> Unit,
    onWipeAllNotes: () -> Unit,
    onSetGeminiApiKey: (String) -> Unit = {},
    onOpenSyncSheet: () -> Unit = {},
    onBackupToDrive: () -> Unit = {},
    onRestoreFromDrive: () -> Unit = {}
) {
    val colors = GlassTheme.colors
    val localContext = androidx.compose.ui.platform.LocalContext.current
    var deleteArmed by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyDraft by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showEditAppNameDialog by remember { mutableStateOf(false) }
    var appNameDraft by remember(settings.customAppName) { mutableStateOf(settings.customAppName) }
    var showResetCustomizationDialog by remember { mutableStateOf(false) }
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

            // Section: Customization & Branding
            SectionHeader(title = "App Customization & Identity")

            // Live Brand Preview Card
            val activePalette = AccentPalettes[settings.accentPalette] ?: AccentPalettes["gold"]!!
            val activeIconKey = if (settings.appIconPreset == "gold") "default" else settings.appIconPreset
            val activeIconItem = LauncherIconsList.find { it.key == activeIconKey } ?: LauncherIconsList[0]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(colors.field)
                    .border(1.dp, colors.hairline, RoundedCornerShape(32.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini Launcher Icon Preview
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = activeIconItem.primaryColor.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(activeIconItem.primaryColor, activeIconItem.secondaryColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "App Icon",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = settings.customAppName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(colors.chipOnBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Preview",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.chipOnTx
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(activePalette.previewColor)
                                )
                                Text(
                                    text = activePalette.name,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Text(text = "•", fontSize = 12.sp, color = colors.textTertiary)

                            Text(
                                text = when (settings.fontFamilyStyle) {
                                    "serif" -> "Serif Font"
                                    "mono" -> "Mono Font"
                                    else -> "Sans Font"
                                },
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Quick Edit Name Icon Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.chipOnBg)
                            .clickable {
                                VibrationHelper.click(localContext)
                                appNameDraft = settings.customAppName
                                showEditAppNameDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit App Name",
                            tint = colors.chipOnTx,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 1. App Name Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        VibrationHelper.click(localContext)
                        appNameDraft = settings.customAppName
                        showEditAppNameDialog = true
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.Edit)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Name & Header",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Currently '${settings.customAppName}' • Tap to rename",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.field)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Change",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }

            // 2. Preset Accent Color Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIcon(icon = Icons.Default.Palette)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preset Accent Colors",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                        Text(
                            text = "Applies across all UI buttons, chips, glow lines, and highlights",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Swatches Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentPalettes.forEach { (key, paletteColors) ->
                        val isSelected = settings.accentPalette == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    VibrationHelper.click(localContext)
                                    onSetAccentPalette(key)
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(
                                        elevation = if (isSelected) 8.dp else 2.dp,
                                        shape = CircleShape,
                                        spotColor = paletteColors.previewColor.copy(alpha = 0.6f)
                                    )
                                    .clip(CircleShape)
                                    .background(paletteColors.previewColor)
                                    .border(
                                        width = if (isSelected) 3.5.dp else 1.5.dp,
                                        color = if (isSelected) (if (colors.isDark) Color.White else Color(0xFF1E1E24)) else colors.hairline,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (key == "gold") Color(0xFF221A00) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = paletteColors.name.split(" ").first(),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.text else colors.textSecondary
                            )
                        }
                    }
                }
            }

            // 3. App Launcher Icon Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIcon(icon = Icons.Default.Brush)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Launcher Icon",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                        Text(
                            text = "Changes your home screen app icon instantly",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Launcher Icons Horizontal Scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LauncherIconsList.forEach { iconItem ->
                        val isSelected = (settings.appIconPreset == iconItem.key) ||
                                (iconItem.key == "default" && settings.appIconPreset == "gold")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    VibrationHelper.click(localContext)
                                    onSetAppIconPreset(iconItem.key)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(
                                        elevation = if (isSelected) 10.dp else 2.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        spotColor = iconItem.primaryColor.copy(alpha = 0.6f)
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(iconItem.primaryColor, iconItem.secondaryColor)
                                        )
                                    )
                                    .border(
                                        width = if (isSelected) 3.5.dp else 1.dp,
                                        color = if (isSelected) (if (colors.isDark) Color.White else Color(0xFF1E1E24)) else colors.hairline,
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = iconItem.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34C759)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = iconItem.name,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.text else colors.textSecondary
                            )
                        }
                    }
                }
            }

            // 4. App Typography Style Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.TextFields)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Typography",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Font family for notes, headers, and menus",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.field)
                        .padding(2.dp)
                ) {
                    ThemeSegmentButton("Sans", settings.fontFamilyStyle == "sans") {
                        onSetFontFamilyStyle("sans")
                    }
                    ThemeSegmentButton("Serif", settings.fontFamilyStyle == "serif") {
                        onSetFontFamilyStyle("serif")
                    }
                    ThemeSegmentButton("Mono", settings.fontFamilyStyle == "mono") {
                        onSetFontFamilyStyle("mono")
                    }
                }
            }

            // 5. Reset Customization to Defaults
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        VibrationHelper.click(localContext)
                        showResetCustomizationDialog = true
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(icon = Icons.Default.RestartAlt)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reset Customization",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "Restore default name, amber gold color, icon, and font",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.field)
                        .padding(2.dp)
                ) {
                    ThemeSegmentButton("Auto", settings.theme == "auto") { onSetTheme("auto") }
                    ThemeSegmentButton("Light", settings.theme == "light") { onSetTheme("light") }
                    ThemeSegmentButton("Dark", settings.theme == "dark") { onSetTheme("dark") }
                }
            }

            val localContext = androidx.compose.ui.platform.LocalContext.current

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
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.field)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(24.dp))
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
                            .clip(RoundedCornerShape(24.dp))
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
                        .clip(RoundedCornerShape(24.dp))
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
                        .clip(RoundedCornerShape(24.dp))
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
                    .clip(RoundedCornerShape(24.dp))
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
                                .clip(RoundedCornerShape(24.dp))
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

            if (showEditAppNameDialog) {
                AlertDialog(
                    onDismissRequest = { showEditAppNameDialog = false },
                    title = {
                        Text(
                            text = "Customize App Name",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Enter the name you want displayed across headers, tabs, and notifications.",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                            OutlinedTextField(
                                value = appNameDraft,
                                onValueChange = { if (it.length <= 25) appNameDraft = it },
                                label = { Text("App Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Quick suggestions:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textTertiary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("HTML Notes", "Glass Vault", "Zen Notes", "Code Diary", "Pocket Notes").forEach { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(colors.field)
                                            .border(1.dp, colors.hairline, RoundedCornerShape(24.dp))
                                            .clickable {
                                                appNameDraft = suggestion
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.text
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val finalName = appNameDraft.trim().ifBlank { "HTML Notes" }
                                onSetCustomAppName(finalName)
                                showEditAppNameDialog = false
                            }
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = colors.accent)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showEditAppNameDialog = false
                            }
                        ) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                    }
                )
            }

            if (showResetCustomizationDialog) {
                AlertDialog(
                    onDismissRequest = { showResetCustomizationDialog = false },
                    title = {
                        Text(
                            text = "Reset Customization?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    },
                    text = {
                        Text(
                            text = "This will restore the default app name ('HTML Notes'), Amber Gold accent color, default launcher icon, and standard sans-serif font.",
                            fontSize = 13.5.sp,
                            color = colors.textSecondary,
                            lineHeight = 19.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onResetCustomization()
                                appNameDraft = "HTML Notes"
                                showResetCustomizationDialog = false
                            }
                        ) {
                            Text("Reset", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showResetCustomizationDialog = false }
                        ) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                    }
                )
            }

            // Section: Library & Backup
            SectionHeader(title = "Library & Cloud Sync")

            val googleEmail = settings.googleAccountEmail
            SettingsActionRow(
                icon = Icons.Default.Cloud,
                label = if (googleEmail != null) "Google Drive ($googleEmail)" else "Google Drive Cloud Backup",
                sub = if (googleEmail != null) "Manage Google Drive cloud backup & restore" else "Connect Google account for cloud sync",
                onClick = { onDismiss(); onOpenSyncSheet() }
            )

            if (googleEmail != null) {
                SettingsActionRow(
                    icon = Icons.Default.CloudDone,
                    label = "Back up to Google Drive now",
                    sub = if (settings.lastGoogleDriveBackupTime > 0L) {
                        "Last backup: ${DateFormatter.fmtClock(settings.lastGoogleDriveBackupTime)}"
                    } else {
                        "Upload entire library to Google Drive"
                    },
                    onClick = { onDismiss(); onBackupToDrive() }
                )
            }

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
                        .clip(RoundedCornerShape(24.dp))
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
                        .clip(RoundedCornerShape(24.dp))
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
                    .clip(RoundedCornerShape(24.dp))
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
            .clip(RoundedCornerShape(24.dp))
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
    val view = LocalView.current
    val themeTransition = LocalThemeTransition.current
    var buttonCenter by remember { mutableStateOf(Offset.Unspecified) }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val rootPos = coords.positionInRoot()
                val sz = coords.size
                buttonCenter = Offset(rootPos.x + sz.width / 2f, rootPos.y + sz.height / 2f)
            }
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) colors.card else Color.Transparent)
            .clickable {
                VibrationHelper.click(context)
                val origin = if (buttonCenter != Offset.Unspecified) buttonCenter else Offset(500f, 500f)
                themeTransition.prepareTransition(origin, view, colors.bg)
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
            .clip(RoundedCornerShape(24.dp))
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

private data class LauncherIconItem(
    val key: String,
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

private val LauncherIconsList = listOf(
    LauncherIconItem("default", "Amber", Color(0xFFF2B90C), Color(0xFF8F6400)),
    LauncherIconItem("emerald", "Emerald", Color(0xFF10B981), Color(0xFF047857)),
    LauncherIconItem("blue", "Royal Blue", Color(0xFF3B82F6), Color(0xFF1D4ED8)),
    LauncherIconItem("rose", "Rose", Color(0xFFF43F5E), Color(0xFFBE123C)),
    LauncherIconItem("purple", "Purple", Color(0xFF8B5CF6), Color(0xFF6D28D9)),
    LauncherIconItem("crimson", "Crimson", Color(0xFFEF4444), Color(0xFFB91C1C)),
    LauncherIconItem("cyan", "Cyan", Color(0xFF06B6D4), Color(0xFF0E7490)),
    LauncherIconItem("sunset", "Sunset", Color(0xFFF97316), Color(0xFFC2410C))
)
