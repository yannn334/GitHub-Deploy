package com.example.githubdeploy.data.remote

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * GitHub REST API v3 surface used by the app:
 * - get repository info
 * - list releases
 * - create a release
 * - upload a release asset (uses the release's own upload_url host)
 * - get repository contents (path listing)
 */
interface GitHubApiService {

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRepoResponse>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<ReleaseResponse>>

    @POST("repos/{owner}/{repo}/releases")
    suspend fun createRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateReleaseRequest
    ): Response<ReleaseResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): Response<List<RepoContentResponse>>

    // The release's upload_url is a full URL on a different host (uploads.github.com),
    // so we pass it directly via @Url rather than a relative path.
    @POST
    suspend fun uploadReleaseAsset(
        @Url uploadUrl: String,
        @Body file: RequestBody
    ): Response<UploadAssetResponse>
}
