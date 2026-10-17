// Version 1.14 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import com.flashchip.flashgallery.ui.components.AboutDialog
import com.flashchip.flashgallery.ui.components.AppCreditFooter
import com.flashchip.flashgallery.ui.components.CodeTextViewerDialog
import com.flashchip.flashgallery.ui.components.ExifBottomSheet
import com.flashchip.flashgallery.ui.components.OtaUpdateDialog
import com.flashchip.flashgallery.ui.components.PdfViewerDialog
import com.flashchip.flashgallery.ui.screens.ApksScreen
import com.flashchip.flashgallery.ui.screens.DocsScreen
import com.flashchip.flashgallery.ui.screens.MediaViewerDialog
import com.flashchip.flashgallery.ui.screens.PhotosScreen
import com.flashchip.flashgallery.ui.theme.BgDark
import com.flashchip.flashgallery.ui.theme.BorderDark
import com.flashchip.flashgallery.ui.theme.FlashAmber
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.FlashGalleryTheme
import com.flashchip.flashgallery.ui.theme.FlashRed
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.SurfaceDarkVariant
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import com.flashchip.flashgallery.ui.theme.TextTertiary
import com.flashchip.flashgallery.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlashGalleryTheme {
                FlashGalleryApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val mediaGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.setPermissionGranted(mediaGranted)

        val allFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.setAllFilesPermissionGranted(allFilesGranted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashGalleryApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.setPermissionGranted(allGranted)
    }

    // Storage Management (All files access) launcher for Android 11+
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val granted = Environment.isExternalStorageManager()
            viewModel.setAllFilesPermissionGranted(granted)
        }
    }

    val onRequestAllFiles = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                manageStorageLauncher.launch(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        }
    }

    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.14"
        } catch (_: Exception) {
            "1.14"
        }
    }

    val onShareSelectedMedia = {
        val selected = viewModel.getSelectedMediaItems()
        if (selected.isNotEmpty()) {
            try {
                val shareIntent = if (selected.size == 1) {
                    val item = selected.first()
                    Intent(Intent.ACTION_SEND).apply {
                        type = if (item.isVideo) "video/*" else "image/*"
                        putExtra(Intent.EXTRA_STREAM, item.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    val uris = ArrayList<Uri>(selected.size)
                    var hasVideo = false
                    var hasImage = false
                    for (item in selected) {
                        uris.add(item.uri)
                        if (item.isVideo) hasVideo = true else hasImage = true
                    }
                    val mimeType = when {
                        hasImage && !hasVideo -> "image/*"
                        hasVideo && !hasImage -> "video/*"
                        else -> "*/*"
                    }
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = mimeType
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir multimedia"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error al compartir: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val needsPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                needsPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                needsPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                needsPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (needsPermissions.isNotEmpty()) {
            permissionsLauncher.launch(needsPermissions.toTypedArray())
        } else {
            viewModel.setPermissionGranted(true)
        }

        val allFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.setAllFilesPermissionGranted(allFilesGranted)
    }

    // Manejo de boton Atras del movil para evitar que se minimice la app
    BackHandler(enabled = uiState.activePdfDoc != null) {
        viewModel.closePdfViewer()
    }
    BackHandler(enabled = uiState.activeCodeDoc != null) {
        viewModel.closeCodeViewer()
    }
    BackHandler(enabled = uiState.activeViewerItem != null) {
        viewModel.openViewer(null)
    }
    BackHandler(enabled = uiState.isAboutDialogVisible) {
        viewModel.showAboutDialog(false)
    }
    BackHandler(enabled = uiState.isOtaDialogVisible) {
        viewModel.dismissOtaDialog()
    }
    BackHandler(enabled = uiState.isExifSheetVisible) {
        viewModel.closeExifSheet()
    }
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearMediaSelection()
    }
    BackHandler(enabled = uiState.selectedAppFolder != null) {
        viewModel.openAppFolder(null)
    }
    BackHandler(enabled = uiState.currentTab == 0 && uiState.photoViewMode == 1) {
        viewModel.setPhotoViewMode(0)
    }
    BackHandler(enabled = uiState.currentTab != 0) {
        viewModel.selectTab(0)
    }

    Scaffold(
        topBar = {
            Column {
                if (uiState.isSelectionMode) {
                    // Selection Action Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = "${uiState.selectedMediaIds.size} seleccionados",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearMediaSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar selección",
                                    tint = TextPrimary
                                )
                            }
                        },
                        actions = {
                            // Compartir seleccionados
                            IconButton(onClick = onShareSelectedMedia) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir seleccionados",
                                    tint = FlashCyan
                                )
                            }
                            // Seleccionar todo
                            IconButton(onClick = { viewModel.selectAllMedia(true) }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Seleccionar todo",
                                    tint = FlashCyan
                                )
                            }
                            // Mover / Cambiar de carpeta
                            IconButton(onClick = { viewModel.showMoveFolderDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = "Mover a carpeta",
                                    tint = FlashCyan
                                )
                            }
                            // Eliminar seleccionados
                            IconButton(onClick = { viewModel.confirmDeleteSelectedMedia(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar seleccionados",
                                    tint = FlashRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                    )
                } else {
                    // Standard Top Bar
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceDarkVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = FlashCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "FlashGallery",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "v$appVersion • by WoRFReD",
                                        color = FlashCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.refreshAll() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar",
                                    tint = FlashCyan
                                )
                            }
                            IconButton(onClick = { viewModel.showAboutDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Acerca de y créditos",
                                    tint = FlashCyan
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                    )
                }

                // Top Navigation Tabs (Pestaña 1: Fotos, Pestaña 2: APKs, Pestaña 3: Documentos)
                TabRow(
                    selectedTabIndex = uiState.currentTab,
                    containerColor = SurfaceDark,
                    contentColor = FlashCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab]),
                            color = FlashCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = uiState.currentTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Text(
                                text = "Fotos y Vídeos",
                                fontWeight = if (uiState.currentTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        selectedContentColor = FlashCyan,
                        unselectedContentColor = TextSecondary
                    )

                    Tab(
                        selected = uiState.currentTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Text(
                                text = "APKs",
                                fontWeight = if (uiState.currentTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        selectedContentColor = FlashCyan,
                        unselectedContentColor = TextSecondary
                    )

                    Tab(
                        selected = uiState.currentTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        text = {
                            Text(
                                text = "Documentos",
                                fontWeight = if (uiState.currentTab == 2) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        selectedContentColor = FlashCyan,
                        unselectedContentColor = TextSecondary
                    )
                }
            }
        },
        containerColor = BgDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission request banner if not granted
            if (!uiState.hasStoragePermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = FlashCyan,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permiso de Almacenamiento Requerido",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FlashGallery necesita acceso al almacenamiento para organizar tus fotos cronológicamente, clasificar APKs y buscar documentos.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    manageStorageLauncher.launch(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    manageStorageLauncher.launch(intent)
                                }
                            } else {
                                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionsLauncher.launch(list)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlashCyan,
                            contentColor = BgDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Conceder Permisos", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                when (uiState.currentTab) {
                    0 -> PhotosScreen(
                        timelineItems = uiState.timelineItems,
                        appFolders = uiState.appFolders,
                        selectedAppFolder = uiState.selectedAppFolder,
                        photoViewMode = uiState.photoViewMode,
                        gridColumns = uiState.gridColumns,
                        selectedMediaIds = uiState.selectedMediaIds,
                        isSelectionMode = uiState.isSelectionMode,
                        isLoading = uiState.isLoadingMedia,
                        onViewModeChange = { viewModel.setPhotoViewMode(it) },
                        onToggleColumns = { viewModel.toggleGridColumns() },
                        onSelectFolder = { viewModel.openAppFolder(it) },
                        onMediaClick = { viewModel.openViewer(it) },
                        onToggleSelectMedia = { viewModel.toggleSelectMedia(it) },
                        onSelectMonthBatch = { viewModel.selectMonthBatch(it) }
                    )

                    1 -> ApksScreen(
                        apkList = uiState.apkList,
                        isLoading = uiState.isLoadingApks,
                        apkToDelete = uiState.apkToDelete,
                        isDeleteDialogVisible = uiState.isDeleteApkDialogVisible,
                        postInstallApk = uiState.postInstallApk,
                        hasAllFilesPermission = uiState.hasAllFilesPermission,
                        onRequestAllFilesPermission = onRequestAllFiles,
                        onInstall = { viewModel.installApk(it) },
                        onConfirmDelete = { viewModel.confirmDeleteApk(it) },
                        onExecuteDelete = { viewModel.executeDeleteApk() },
                        onDismissPostInstall = { viewModel.dismissPostInstallPrompt() }
                    )

                    2 -> DocsScreen(
                        docList = uiState.docList,
                        selectedCategory = uiState.selectedDocCategory,
                        searchQuery = uiState.docSearchQuery,
                        isLoading = uiState.isLoadingDocs,
                        docToDelete = uiState.docToDelete,
                        isDeleteDialogVisible = uiState.isDeleteDocDialogVisible,
                        hasAllFilesPermission = uiState.hasAllFilesPermission,
                        onRequestAllFilesPermission = onRequestAllFiles,
                        onCategoryChange = { viewModel.setDocCategory(it) },
                        onSearchChange = { viewModel.setDocSearchQuery(it) },
                        onOpenDoc = { viewModel.openDocument(it) },
                        onConfirmDelete = { viewModel.confirmDeleteDoc(it) },
                        onExecuteDelete = { viewModel.executeDeleteDoc() }
                    )
                }
            }
        }
    }

    // Fullscreen Media Viewer
    if (uiState.activeViewerItem != null && uiState.viewerMediaList.isNotEmpty()) {
        MediaViewerDialog(
            mediaList = uiState.viewerMediaList,
            initialIndex = uiState.viewerInitialIndex,
            onDismiss = { viewModel.openViewer(null) },
            onShowExif = { item -> viewModel.loadExif(item) },
            onDelete = { item -> viewModel.deleteViewerItem(item) }
        )
    }

    // EXIF Bottom Sheet
    if (uiState.isExifSheetVisible) {
        ExifBottomSheet(
            exif = uiState.activeExifMetadata,
            onDismiss = { viewModel.closeExifSheet() }
        )
    }

    // Mandatory About / Credits Dialog
    if (uiState.isAboutDialogVisible) {
        AboutDialog(
            totalPhotos = uiState.totalPhotosCount,
            totalVideos = uiState.totalVideosCount,
            totalApks = uiState.apkList.size,
            totalDocs = uiState.docList.size,
            isCheckingUpdates = uiState.isCheckingOta,
            onCheckUpdates = {
                viewModel.checkForUpdates(manual = true) {
                    Toast.makeText(context, "Tienes la versión más reciente (v$appVersion)", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { viewModel.showAboutDialog(false) }
        )
    }

    // Diálogo de actualización OTA
    if (uiState.isOtaDialogVisible && uiState.otaReleaseInfo != null) {
        OtaUpdateDialog(
            releaseInfo = uiState.otaReleaseInfo!!,
            isDownloading = uiState.isOtaDownloading,
            downloadProgress = uiState.otaDownloadProgress,
            downloadedBytes = uiState.otaDownloadedBytes,
            totalBytes = uiState.otaTotalBytes,
            onStartDownload = { viewModel.startOtaDownload() },
            onDismiss = { viewModel.dismissOtaDialog() }
        )
    }

    // Visor nativo de documentos PDF
    if (uiState.activePdfDoc != null) {
        PdfViewerDialog(
            doc = uiState.activePdfDoc!!,
            onDismiss = { viewModel.closePdfViewer() },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uiState.activePdfDoc!!.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
            },
            onOpenExternal = {
                viewModel.openDocumentExternal(uiState.activePdfDoc!!)
            }
        )
    }

    // Visor de código y texto estilo Notepad++ con resaltado de sintaxis
    if (uiState.activeCodeDoc != null) {
        CodeTextViewerDialog(
            doc = uiState.activeCodeDoc!!,
            onDismiss = { viewModel.closeCodeViewer() },
            onOpenExternal = {
                viewModel.openDocumentExternal(uiState.activeCodeDoc!!)
            }
        )
    }

    // Diálogo de confirmación para eliminar elementos seleccionados
    if (uiState.isDeleteSelectedMediaDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmDeleteSelectedMedia(false) },
            title = {
                Text(
                    text = "¿Eliminar ${uiState.selectedMediaIds.size} elementos?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Se eliminarán permanentemente los ${uiState.selectedMediaIds.size} elementos seleccionados de tu dispositivo.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteSelectedMedia() },
                    colors = ButtonDefaults.textButtonColors(contentColor = FlashRed)
                ) {
                    Text(text = "Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteSelectedMedia(false) },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text(text = "Cancelar")
                }
            }
        )
    }

    // Diálogo para Cambiar / Mover elementos seleccionados a otra carpeta
    if (uiState.isMoveFolderDialogVisible) {
        var newFolderName by remember { mutableStateOf("") }
        var isCreatingNewFolder by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.showMoveFolderDialog(false) },
            title = {
                Text(
                    text = "Mover a carpeta (${uiState.selectedMediaIds.size} seleccionados)",
                    color = FlashCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isCreatingNewFolder) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Nombre de la nueva carpeta") },
                            placeholder = { Text("Ej. Vacaciones, Favoritas...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FlashCyan,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isCreatingNewFolder = false }) {
                                Text("Volver a la lista", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newFolderName.isNotBlank()) {
                                        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                                        val target = File(dcim, newFolderName.trim())
                                        viewModel.moveSelectedMediaToFolder(target)
                                    }
                                },
                                enabled = newFolderName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = FlashCyan, contentColor = BgDark)
                            ) {
                                Text("Crear y Mover", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { isCreatingNewFolder = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FlashCyan.copy(alpha = 0.15f), contentColor = FlashCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Crear nueva carpeta", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Carpetas existentes en el dispositivo:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                            items(uiState.availableFolders) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.moveSelectedMediaToFolder(folder) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = FlashCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = folder.parent ?: "",
                                            color = TextTertiary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(color = BorderDark.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showMoveFolderDialog(false) },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text(text = "Cancelar")
                }
            }
        )
    }
}
