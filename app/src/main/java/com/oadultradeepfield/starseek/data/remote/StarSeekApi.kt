package com.oadultradeepfield.starseek.data.remote

import com.oadultradeepfield.starseek.data.remote.dto.JobStatusResponse
import com.oadultradeepfield.starseek.data.remote.dto.ObjectDetailResponse
import com.oadultradeepfield.starseek.data.remote.dto.SolveResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface StarSeekApi {
  @Multipart
  @POST("api/solve")
  suspend fun uploadImage(@Part image: MultipartBody.Part): SolveResponse

  @GET("api/solve/{jobId}")
  suspend fun getJobStatus(@Path("jobId") jobId: String): JobStatusResponse

  @GET("api/object/{name}")
  suspend fun getObjectDetail(@Path("name") name: String): ObjectDetailResponse
}
