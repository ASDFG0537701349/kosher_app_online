package com.example.kosher_app_online

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StoreUiState {
    object Loading : StoreUiState()
    data class Success(val apps: List<AppItem>, val categories: List<String>) : StoreUiState()
    data class Error(val message: String) : StoreUiState()
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val downloadManager = AppDownloadManager(application)
    private val mdmInstaller = MdmInstaller(application)
    
    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()
    
    // Track download IDs for monitoring
    private val downloadIds = mutableMapOf<String, Long>()
    
    init {
        loadApps()
    }
    
    fun loadApps() {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = StoreUiState.Loading
            
            try {
                // Switch to IO for network operation
                val remoteApps = withContext(Dispatchers.IO) {
                    repository.fetchApps()
                }
                
                if (remoteApps.isNotEmpty()) {
                    val categories = remoteApps.map { it.category }.distinct().sorted()
                    val appItems = remoteApps.map { remoteApp ->
                        AppItem(
                            name = remoteApp.name,
                            packageName = remoteApp.packageName,
                            version = remoteApp.version,
                            versionCode = remoteApp.versionCode,
                            downloadUrl = remoteApp.downloadUrl,
                            iconUrl = remoteApp.iconUrl,
                            category = remoteApp.category
                        )
                    }
                    _uiState.value = StoreUiState.Success(appItems, categories)
                } else {
                    _uiState.value = StoreUiState.Error("לא נמצאו אפליקציות בשרת")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = StoreUiState.Error("שגיאה בטעינת הנתונים: ${e.message}")
            }
        }
    }
    
    fun getAppsByCategory(category: String): List<AppItem> {
        return when (val state = _uiState.value) {
            is StoreUiState.Success -> {
                if (category == "הכל") {
                    state.apps
                } else {
                    state.apps.filter { it.category == category }
                }
            }
            else -> emptyList()
        }
    }
    
    fun getCategories(): List<String> {
        return when (val state = _uiState.value) {
            is StoreUiState.Success -> state.categories
            else -> emptyList()
        }
    }
    
    fun downloadApp(app: AppItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update app to downloading state
            updateAppDownloadingState(app.packageName, true, 0)
            
            try {
                // Start download using DownloadManager
                val downloadId = downloadManager.enqueueDownload(app)
                
                if (downloadId != -1L) {
                    // Store the download ID for later retrieval
                    downloadIds[app.packageName] = downloadId
                    
                    // Monitor download completion
                    monitorDownload(downloadId, app)
                } else {
                    updateAppDownloadingState(app.packageName, false, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateAppDownloadingState(app.packageName, false, 0)
            }
        }
    }
    
    private fun monitorDownload(downloadId: Long, app: AppItem) {
        viewModelScope.launch(Dispatchers.IO) {
            var completed = false
            var attempts = 0
            val maxAttempts = 30 // Check for 30 seconds
            var cursor: android.database.Cursor? = null
            
            try {
                while (!completed && attempts < maxAttempts) {
                    kotlinx.coroutines.delay(1000) // Check every second
                    
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    cursor = (getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).query(query)
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex >= 0) {
                            val status = cursor.getInt(statusIndex)
                            
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    completed = true
                                    val uri = downloadManager.getUriForDownloadedFile(downloadId)
                                    if (uri != null) {
                                        // Install the APK
                                        val success = mdmInstaller.installPackage(uri)
                                        if (!success) {
                                            // Fallback to standard installer
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/vnd.android.package-archive")
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            getApplication<Application>().startActivity(intent)
                                        }
                                    }
                                    updateAppDownloadingState(app.packageName, false, 0)
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    completed = true
                                    updateAppDownloadingState(app.packageName, false, 0)
                                }
                                DownloadManager.STATUS_PENDING -> {
                                    // Still downloading, continue monitoring
                                }
                                DownloadManager.STATUS_RUNNING -> {
                                    // Get progress
                                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                    if (bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0) {
                                        val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
                                        val bytesTotal = cursor.getInt(bytesTotalIndex)
                                        if (bytesTotal > 0) {
                                            val progress = (bytesDownloaded * 100) / bytesTotal
                                            updateAppDownloadingState(app.packageName, true, progress)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    attempts++
                }
            } finally {
                cursor?.close()
            }
        }
    }
    
    private fun updateAppDownloadingState(packageName: String, isDownloading: Boolean, progress: Int) {
        val currentState = _uiState.value
        if (currentState is StoreUiState.Success) {
            val updatedApps = currentState.apps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isDownloading = isDownloading, downloadProgress = progress)
                } else {
                    app
                }
            }
            // This function is called from IO dispatcher, so we need to switch to Main
            viewModelScope.launch(Dispatchers.Main) {
                _uiState.value = StoreUiState.Success(updatedApps, currentState.categories)
            }
        }
    }
}