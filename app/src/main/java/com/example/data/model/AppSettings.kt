package com.example.data.model

data class AppSettings(
    val theme: String = "auto", // "auto", "light", "dark"
    val reduceTransparency: Boolean = false,
    val readingFontSize: Int = 17, // 13..24
    val autoSync: Boolean = true,
    val syncFolderUri: String? = null,
    val syncFolderName: String? = null,
    val lastSyncTime: Long = 0L,
    val sortOrder: String = "edited", // "edited", "created", "title"
    val viewMode: String = "list", // "list", "grid"
    val biometricLockEnabled: Boolean = false,
    val pinCode: String = "",
    val readerMode: String = "html", // "html" or "pdf"
    val pdfPageMode: String = "continuous", // "continuous", "single"
    val pdfColorFilter: String = "default", // "default", "sepia", "dark", "invert"
    val pdfRenderQuality: String = "sharp", // "sharp", "balanced"
    val geminiApiKey: String = "",
    val hapticsEnabled: Boolean = true,
    val googleAccountEmail: String? = null,
    val googleAccountName: String? = null,
    val googleAccountPhoto: String? = null,
    val googleDriveAutoBackup: Boolean = true,
    val lastGoogleDriveBackupTime: Long = 0L,
    val deleteFromStorageWhenDeleted: Boolean = true,
    val customAppName: String = "HTML Notes",
    val accentPalette: String = "gold",
    val appIconPreset: String = "default",
    val fontFamilyStyle: String = "sans"
)
