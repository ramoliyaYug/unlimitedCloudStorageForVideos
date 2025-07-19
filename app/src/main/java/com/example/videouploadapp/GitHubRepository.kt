package com.example.videouploadapp

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit

class GitHubRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubApi::class.java)

    suspend fun uploadFile(fileName: String, request: UploadRequest) {
        api.uploadFile(
            token = "token $TOKEN",
            owner = OWNER,
            repo = REPO,
            path = "$PATH/$fileName",
            request = request
        )
    }

    suspend fun getFiles(): List<GitHubFile> {
        return api.getContents(
            token = "token $TOKEN",
            owner = OWNER,
            repo = REPO,
            path = PATH
        )
    }

    companion object {
        private const val TOKEN = ""
        private const val OWNER = "ramoliyaYug"
        private const val REPO = "upload"
        private const val BRANCH = "main"
        private const val PATH = "videos"
    }
}

interface GitHubApi {
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun uploadFile(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: UploadRequest
    )

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): List<GitHubFile>
}

data class UploadRequest(
    val message: String,
    val content: String,
    val branch: String
)

data class GitHubFile(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val download_url: String
) {
    val downloadUrl: String get() = download_url
}
