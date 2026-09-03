package com.example.githubdeploy.data.remote

import com.google.gson.annotations.SerializedName

data class GitHubRepoResponse(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("clone_url") val cloneUrl: String,
    @SerializedName("default_branch") val defaultBranch: String,
    val description: String?
)

data class CreateReleaseRequest(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("target_commitish") val targetCommitish: String = "main",
    val name: String,
    val body: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false
)

data class ReleaseResponse(
    val id: Long,
    @SerializedName("tag_name") val tagName: String,
    val name: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("upload_url") val uploadUrl: String
)

data class UploadAssetResponse(
    val id: Long,
    val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

data class RepoContentResponse(
    val name: String,
    val path: String,
    val type: String,
    @SerializedName("download_url") val downloadUrl: String?
)
