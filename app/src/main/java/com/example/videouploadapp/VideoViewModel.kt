package com.example.videouploadapp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.util.Base64

class VideoViewModel : ViewModel() {
    private val repository = GitHubRepository()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _videos = MutableStateFlow<List<GitHubFile>>(emptyList())
    val videos: StateFlow<List<GitHubFile>> = _videos.asStateFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()

    fun uploadVideo(context: Context, videoUri: Uri, videoName: String) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Loading

                val tempFile = File(context.cacheDir, "temp_video.mp4")

                context.contentResolver.openInputStream(videoUri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val fileSizeInMB = tempFile.length() / (1024 * 1024)
                if (fileSizeInMB > 25) {
                    throw Exception("File too large ($fileSizeInMB MB). Please use a smaller video (max 25MB)")
                }

                val fileName = "$videoName.mp4"
                val base64Content = encodeFileToBase64(tempFile)

                val uploadRequest = UploadRequest(
                    message = "Upload video: $fileName",
                    content = base64Content,
                    branch = "main"
                )

                repository.uploadFile(fileName, uploadRequest)
                _uploadState.value = UploadState.Success

                tempFile.delete()
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    private fun encodeFileToBase64(file: File): String {
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: OutOfMemoryError) {
            throw Exception("File too large to process. Please use a smaller video.")
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            try {
                _loadingState.value = LoadingState.Loading
                val files = repository.getFiles()
                _videos.value = files.filter { it.name.endsWith(".mp4") }
                _loadingState.value = LoadingState.Success
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load videos")
            }
        }
    }

    fun setCurrentVideo(url: String) {
        _currentVideoUrl.value = url
    }
}

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}