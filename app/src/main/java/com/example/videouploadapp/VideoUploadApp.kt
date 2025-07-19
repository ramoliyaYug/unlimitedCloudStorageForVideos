package com.example.videouploadapp

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoUploadApp() {
    val viewModel: VideoViewModel = viewModel()
    val context = LocalContext.current
    var videoName by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var currentScreen by remember { mutableStateOf("upload") }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { currentScreen = "upload" },
                colors = if (currentScreen == "upload")
                    ButtonDefaults.buttonColors()
                else
                    ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Upload")
            }

            Button(
                onClick = {
                    currentScreen = "videos"
                    viewModel.loadVideos()
                },
                colors = if (currentScreen == "videos")
                    ButtonDefaults.buttonColors()
                else
                    ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Videos")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (currentScreen) {
            "upload" -> {
                UploadScreen(
                    videoName = videoName,
                    onVideoNameChange = { videoName = it },
                    selectedVideoUri = selectedVideoUri,
                    onPickVideo = { videoPickerLauncher.launch("video/*") },
                    onUpload = {
                        selectedVideoUri?.let { uri ->
                            viewModel.uploadVideo(context, uri, videoName)
                        }
                    },
                    viewModel = viewModel
                )
            }
            "videos" -> {
                VideosScreen(
                    viewModel = viewModel,
                    onVideoClick = { videoUrl ->
                        currentScreen = "player"
                        viewModel.setCurrentVideo(videoUrl)
                    }
                )
            }
            "player" -> {
                VideoPlayerScreen(
                    videoUrl = viewModel.currentVideoUrl.value,
                    onBack = { currentScreen = "videos" }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    videoName: String,
    onVideoNameChange: (String) -> Unit,
    selectedVideoUri: Uri?,
    onPickVideo: () -> Unit,
    onUpload: () -> Unit,
    viewModel: VideoViewModel
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()

    val fileSizeText = selectedVideoUri?.let { uri ->
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE)
        val fileSize = cursor?.let {
            if (it.moveToFirst() && sizeIndex != null && sizeIndex >= 0) {
                it.getLong(sizeIndex)
            } else null
        }
        cursor?.close()

        fileSize?.let { size ->
            val sizeInMB = size / (1024 * 1024)
            "Size: ${sizeInMB}MB"
        }
    }

    val isFileTooLarge = selectedVideoUri?.let { uri ->
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE)
        val fileSize = cursor?.let {
            if (it.moveToFirst() && sizeIndex != null && sizeIndex >= 0) {
                it.getLong(sizeIndex)
            } else null
        }
        cursor?.close()

        fileSize?.let { size ->
            size > 25 * 1024 * 1024
        } ?: false
    } ?: false

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = videoName,
            onValueChange = onVideoNameChange,
            label = { Text("Video Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onPickVideo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick Video from Gallery")
        }

        selectedVideoUri?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = if (isFileTooLarge) {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                } else {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selected: ${it.lastPathSegment}",
                        color = if (isFileTooLarge) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    fileSizeText?.let { sizeText ->
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFileTooLarge) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (isFileTooLarge) {
                        Text(
                            text = "File too large! Maximum size is 25MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Button(
            onClick = onUpload,
            enabled = videoName.isNotBlank() && selectedVideoUri != null &&
                    !isFileTooLarge && uploadState !is UploadState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (uploadState) {
                is UploadState.Loading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Uploading...")
                    }
                }
                else -> {
                    Text("Upload Video")
                }
            }
        }

        when (uploadState) {
            is UploadState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "Upload successful!",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            is UploadState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Upload failed: ${(uploadState as UploadState.Error).message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun VideosScreen(
    viewModel: VideoViewModel,
    onVideoClick: (String) -> Unit
) {
    val videos by viewModel.videos.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()

    Column {
        when (loadingState) {
            is LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoadingState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Error loading videos: ${(loadingState as LoadingState.Error).message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(videos) { video ->
                        VideoItem(
                            video = video,
                            onClick = { onVideoClick(video.downloadUrl) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoItem(
    video: GitHubFile,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = video.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Size: ${video.size} bytes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}