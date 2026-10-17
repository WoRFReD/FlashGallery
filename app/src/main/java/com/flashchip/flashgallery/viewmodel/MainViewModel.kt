// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flashchip.flashgallery.data.ApkRepository
import com.flashchip.flashgallery.data.DocRepository
import com.flashchip.flashgallery.data.MediaRepository
import com.flashchip.flashgallery.data.OtaRepository
import com.flashchip.flashgallery.model.ApkItem
import com.flashchip.flashgallery.model.AppFolder
import com.flashchip.flashgallery.model.DocCategory
import com.flashchip.flashgallery.model.DocItem
import com.flashchip.flashgallery.model.ExifMetadata
import com.flashchip.flashgallery.model.GalleryItem
import com.flashchip.flashgallery.model.MediaItem
import com.flashchip.flashgallery.model.OtaReleaseInfo
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val currentTab: Int = 0, // 0: Fotos, 1: APKs, 2: Documentos
    val hasStoragePermission: Boolean = false,
    val hasAllFilesPermission: Boolean = false,

    // Photos
    val photoViewMode: Int = 0, // 0: Cronológico Todo, 1: De tus apps
    val timelineItems: List<GalleryItem> = emptyList(),
    val appFolders: List<AppFolder> = emptyList(),
    val selectedAppFolder: AppFolder? = null,
    val gridColumns: Int = 3,
    val selectedMediaIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val activeViewerItem: MediaItem? = null,
    val viewerMediaList: List<MediaItem> = emptyList(),
    val viewerInitialIndex: Int = 0,
    val activeExifMetadata: ExifMetadata? = null,
    val isExifSheetVisible: Boolean = false,
    val isLoadingMedia: Boolean = false,
    val isMoveFolderDialogVisible: Boolean = false,
    val availableFolders: List<File> = emptyList(),
    val isDeleteSelectedMediaDialogVisible: Boolean = false,

    // APKs
    val apkList: List<ApkItem> = emptyList(),
    val apkToDelete: ApkItem? = null,
    val isDeleteApkDialogVisible: Boolean = false,
    val postInstallApk: ApkItem? = null,
    val isLoadingApks: Boolean = false,

    // Documents
    val docList: List<DocItem> = emptyList(),
    val selectedDocCategory: DocCategory = DocCategory.ALL,
    val docSearchQuery: String = "",
    val docToDelete: DocItem? = null,
    val isDeleteDocDialogVisible: Boolean = false,
    val isLoadingDocs: Boolean = false,
    val activePdfDoc: DocItem? = null,
    val activeCodeDoc: DocItem? = null,

    // OTA Updates (GitHub Releases)
    val otaReleaseInfo: OtaReleaseInfo? = null,
    val isOtaDialogVisible: Boolean = false,
    val isOtaDownloading: Boolean = false,
    val otaDownloadProgress: Float = 0f,
    val otaDownloadedBytes: Long = 0L,
    val otaTotalBytes: Long = 0L,
    val isCheckingOta: Boolean = false,

    // General
    val isAboutDialogVisible: Boolean = false,
    val totalPhotosCount: Int = 0,
    val totalVideosCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository(application)
    private val apkRepository = ApkRepository(application)
    private val docRepository = DocRepository(application)
    private val otaRepository = OtaRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasStoragePermission = granted) }
        if (granted) {
            refreshAll()
        }
    }

    fun setAllFilesPermissionGranted(granted: Boolean) {
        val previous = _uiState.value.hasAllFilesPermission
        _uiState.update { it.copy(hasAllFilesPermission = granted) }
        // Si el usuario acaba de conceder el permiso de acceso a archivos, refrescamos APKs y Docs
        if (granted && !previous) {
            loadApks()
            loadDocs()
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(currentTab = index) }
        when (index) {
            0 -> if (_uiState.value.timelineItems.isEmpty()) loadMedia()
            1 -> if (_uiState.value.apkList.isEmpty()) loadApks()
            2 -> if (_uiState.value.docList.isEmpty()) loadDocs()
        }
    }

    fun refreshAll() {
        loadMedia()
        loadApks()
        loadDocs()
        checkForUpdates(manual = false)
    }

    // --- Media / Fotos ---
    fun loadMedia() {
        viewModelScope.launch {
            if (_uiState.value.timelineItems.isEmpty()) {
                _uiState.update { it.copy(isLoadingMedia = true) }
            }
            try {
                // Carga progresiva: el primer lote aparece de inmediato en <30ms
                val (timeline, folders) = mediaRepository.loadMediaProgressive { fastTimeline, fastFolders ->
                    if (_uiState.value.timelineItems.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                timelineItems = fastTimeline,
                                appFolders = fastFolders,
                                isLoadingMedia = false
                            )
                        }
                    }
                }
                var photoCount = 0
                var videoCount = 0
                for (item in timeline) {
                    if (item is GalleryItem.Media) {
                        if (item.item.isVideo) videoCount++ else photoCount++
                    }
                }
                _uiState.update {
                    it.copy(
                        timelineItems = timeline,
                        appFolders = folders,
                        totalPhotosCount = photoCount,
                        totalVideosCount = videoCount,
                        isLoadingMedia = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoadingMedia = false) }
            }
        }
    }

    fun setPhotoViewMode(mode: Int) {
        _uiState.update { it.copy(photoViewMode = mode, selectedAppFolder = null) }
    }

    fun openAppFolder(folder: AppFolder?) {
        _uiState.update { it.copy(selectedAppFolder = folder) }
    }

    fun toggleGridColumns() {
        _uiState.update {
            val next = if (it.gridColumns == 3) 5 else 3
            it.copy(gridColumns = next)
        }
    }

    fun toggleSelectMedia(item: MediaItem) {
        _uiState.update { state ->
            val set = state.selectedMediaIds.toMutableSet()
            if (set.contains(item.id)) {
                set.remove(item.id)
            } else {
                set.add(item.id)
            }
            state.copy(
                selectedMediaIds = set,
                isSelectionMode = set.isNotEmpty()
            )
        }
    }

    fun selectMonthBatch(monthItem: GalleryItem.MonthHeader) {
        _uiState.update { state ->
            val set = state.selectedMediaIds.toMutableSet()
            val monthIds = monthItem.mediaList.map { it.id }
            val allSelected = monthIds.all { set.contains(it) }

            if (allSelected) {
                set.removeAll(monthIds.toSet())
            } else {
                set.addAll(monthIds)
            }
            state.copy(
                selectedMediaIds = set,
                isSelectionMode = set.isNotEmpty()
            )
        }
    }

    fun clearMediaSelection() {
        _uiState.update { it.copy(selectedMediaIds = emptySet(), isSelectionMode = false) }
    }

    fun openViewer(item: MediaItem?, list: List<MediaItem>? = null) {
        if (item == null) {
            _uiState.update {
                it.copy(
                    activeViewerItem = null,
                    viewerMediaList = emptyList(),
                    viewerInitialIndex = 0,
                    activeExifMetadata = null,
                    isExifSheetVisible = false
                )
            }
            return
        }

        val targetList = list ?: if (_uiState.value.selectedAppFolder != null) {
            _uiState.value.selectedAppFolder!!.items
        } else {
            _uiState.value.timelineItems.filterIsInstance<GalleryItem.Media>().map { it.item }
        }

        val index = targetList.indexOfFirst { it.id == item.id }.let { if (it >= 0) it else 0 }

        _uiState.update {
            it.copy(
                activeViewerItem = item,
                viewerMediaList = targetList,
                viewerInitialIndex = index,
                activeExifMetadata = null,
                isExifSheetVisible = false
            )
        }
    }

    fun loadExif(item: MediaItem) {
        viewModelScope.launch {
            val exif = mediaRepository.getExifMetadata(item)
            _uiState.update {
                it.copy(activeExifMetadata = exif, isExifSheetVisible = true)
            }
        }
    }

    fun closeExifSheet() {
        _uiState.update { it.copy(isExifSheetVisible = false) }
    }

    fun deleteViewerItem(item: MediaItem) {
        viewModelScope.launch {
            val success = mediaRepository.deleteMedia(item)
            if (success) {
                val currentList = _uiState.value.viewerMediaList.filter { it.id != item.id }
                if (currentList.isEmpty()) {
                    openViewer(null)
                } else {
                    val nextItem = currentList.firstOrNull()
                    _uiState.update {
                        it.copy(
                            activeViewerItem = nextItem,
                            viewerMediaList = currentList,
                            viewerInitialIndex = 0
                        )
                    }
                }
                loadMedia()
            }
        }
    }

    fun selectAllMedia(select: Boolean) {
        _uiState.update { state ->
            val allIds = if (state.selectedAppFolder != null) {
                state.selectedAppFolder.items.map { it.id }.toSet()
            } else {
                state.timelineItems.filterIsInstance<GalleryItem.Media>().map { it.item.id }.toSet()
            }
            state.copy(
                selectedMediaIds = if (select) allIds else emptySet(),
                isSelectionMode = select && allIds.isNotEmpty()
            )
        }
    }

    fun confirmDeleteSelectedMedia(show: Boolean) {
        _uiState.update { it.copy(isDeleteSelectedMediaDialogVisible = show) }
    }

    fun showMoveFolderDialog(show: Boolean) {
        if (show) {
            viewModelScope.launch {
                val folders = mediaRepository.getAvailableFolders()
                _uiState.update {
                    it.copy(
                        isMoveFolderDialogVisible = true,
                        availableFolders = folders
                    )
                }
            }
        } else {
            _uiState.update { it.copy(isMoveFolderDialogVisible = false) }
        }
    }

    fun moveSelectedMediaToFolder(targetFolder: File) {
        viewModelScope.launch {
            val ids = _uiState.value.selectedMediaIds
            val all = _uiState.value.selectedAppFolder?.items
                ?: _uiState.value.timelineItems.filterIsInstance<GalleryItem.Media>().map { it.item }
            val itemsToMove = all.filter { ids.contains(it.id) }

            mediaRepository.moveMedia(itemsToMove, targetFolder)
            _uiState.update {
                it.copy(
                    isMoveFolderDialogVisible = false,
                    selectedMediaIds = emptySet(),
                    isSelectionMode = false
                )
            }
            loadMedia()
        }
    }

    fun deleteSelectedMedia() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedMediaIds
            val all = _uiState.value.selectedAppFolder?.items
                ?: _uiState.value.timelineItems.filterIsInstance<GalleryItem.Media>().map { it.item }
            for (media in all) {
                if (ids.contains(media.id)) {
                    mediaRepository.deleteMedia(media)
                }
            }
            _uiState.update {
                it.copy(
                    selectedMediaIds = emptySet(),
                    isSelectionMode = false,
                    isDeleteSelectedMediaDialogVisible = false
                )
            }
            loadMedia()
        }
    }

    // --- APKs ---
    fun loadApks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApks = true) }
            val apks = apkRepository.scanApks()
            _uiState.update { it.copy(apkList = apks, isLoadingApks = false) }
        }
    }

    fun installApk(apk: ApkItem) {
        apkRepository.launchInstaller(apk)
        _uiState.update { it.copy(postInstallApk = apk) }
    }

    fun confirmDeleteApk(apk: ApkItem?) {
        _uiState.update {
            it.copy(apkToDelete = apk, isDeleteApkDialogVisible = apk != null)
        }
    }

    fun executeDeleteApk() {
        val apk = _uiState.value.apkToDelete ?: return
        viewModelScope.launch {
            apkRepository.deleteApk(apk)
            _uiState.update {
                it.copy(
                    apkToDelete = null,
                    isDeleteApkDialogVisible = false,
                    postInstallApk = if (it.postInstallApk?.file?.path == apk.file.path) null else it.postInstallApk
                )
            }
            loadApks()
        }
    }

    fun dismissPostInstallPrompt() {
        _uiState.update { it.copy(postInstallApk = null) }
    }

    // --- Documentos ---
    fun loadDocs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDocs = true) }
            val docs = docRepository.scanDocuments()
            _uiState.update { it.copy(docList = docs, isLoadingDocs = false) }
        }
    }

    fun setDocCategory(category: DocCategory) {
        _uiState.update { it.copy(selectedDocCategory = category) }
    }

    fun setDocSearchQuery(query: String) {
        _uiState.update { it.copy(docSearchQuery = query) }
    }

    fun openDocument(doc: DocItem) {
        val ext = doc.extension.lowercase()
        if (ext == "pdf") {
            _uiState.update { it.copy(activePdfDoc = doc) }
        } else if (isCodeOrText(ext)) {
            _uiState.update { it.copy(activeCodeDoc = doc) }
        } else {
            docRepository.openDocument(doc)
        }
    }

    fun closePdfViewer() {
        _uiState.update { it.copy(activePdfDoc = null) }
    }

    fun closeCodeViewer() {
        _uiState.update { it.copy(activeCodeDoc = null) }
    }

    fun openDocumentExternal(doc: DocItem) {
        docRepository.openDocument(doc)
    }

    private fun isCodeOrText(ext: String): Boolean {
        return when (ext) {
            "txt", "log", "md", "csv", "ini", "conf", "properties", "cfg",
            "xml", "json", "html", "htm", "css", "js", "ts", "yaml", "yml",
            "java", "kt", "kts", "py", "c", "cpp", "h", "hpp", "cs", "sql",
            "sh", "bat", "cmd", "gradle", "svg" -> true
            else -> false
        }
    }

    fun confirmDeleteDoc(doc: DocItem?) {
        _uiState.update {
            it.copy(docToDelete = doc, isDeleteDocDialogVisible = doc != null)
        }
    }

    fun executeDeleteDoc() {
        val doc = _uiState.value.docToDelete ?: return
        viewModelScope.launch {
            docRepository.deleteDocument(doc)
            _uiState.update { it.copy(docToDelete = null, isDeleteDocDialogVisible = false) }
            loadDocs()
        }
    }

    // --- Info / Credits ---
    fun showAboutDialog(show: Boolean) {
        _uiState.update { it.copy(isAboutDialogVisible = show) }
    }

    // --- OTA Updates (GitHub Releases) ---
    fun checkForUpdates(manual: Boolean = false, onUpToDate: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOta = true) }
            val release = otaRepository.checkLatestRelease("1.11")
            _uiState.update { it.copy(isCheckingOta = false) }
            if (release != null && release.isNewer) {
                _uiState.update {
                    it.copy(otaReleaseInfo = release, isOtaDialogVisible = true)
                }
            } else if (manual) {
                onUpToDate?.invoke()
            }
        }
    }

    fun startOtaDownload() {
        val release = _uiState.value.otaReleaseInfo ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOtaDownloading = true,
                    otaDownloadProgress = 0f,
                    otaDownloadedBytes = 0L,
                    otaTotalBytes = release.apkSizeBytes
                )
            }
            val apkFile = otaRepository.downloadApk(release.downloadUrl) { progress, downloaded, total ->
                _uiState.update {
                    it.copy(
                        otaDownloadProgress = progress,
                        otaDownloadedBytes = downloaded,
                        otaTotalBytes = total
                    )
                }
            }
            _uiState.update { it.copy(isOtaDownloading = false, isOtaDialogVisible = false) }
            if (apkFile != null && apkFile.exists()) {
                otaRepository.installApk(apkFile)
            }
        }
    }

    fun dismissOtaDialog() {
        if (!_uiState.value.isOtaDownloading) {
            _uiState.update { it.copy(isOtaDialogVisible = false) }
        }
    }
}
