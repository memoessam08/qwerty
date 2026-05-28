package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import retrofit2.Response
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("lessons.json")
    suspend fun getLessons(): Map<String, Lesson>?

    @PUT("lessons/{id}.json")
    suspend fun putLesson(@Path("id") id: String, @Body lesson: Lesson): Lesson

    @DELETE("lessons/{id}.json")
    suspend fun deleteLesson(@Path("id") id: String): Response<Unit>

    @GET("exams.json")
    suspend fun getExams(): Map<String, Exam>?

    @PUT("exams/{id}.json")
    suspend fun putExam(@Path("id") id: String, @Body exam: Exam): Exam

    @DELETE("exams/{id}.json")
    suspend fun deleteExam(@Path("id") id: String): Response<Unit>

    @GET("submissions.json")
    suspend fun getSubmissions(): Map<String, QuizSubmission>?

    @PUT("submissions/{id}.json")
    suspend fun putSubmission(@Path("id") id: String, @Body submission: QuizSubmission): QuizSubmission
}

object RetrofitClient {
    private const val BASE_URL = "https://history-advisor-2026-default-rtdb.firebaseio.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
