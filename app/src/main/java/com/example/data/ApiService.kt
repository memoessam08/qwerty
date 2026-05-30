package com.example.data

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import retrofit2.Response
import java.util.concurrent.TimeUnit

interface ApiService {
    // 1. Lessons
    @GET("rest/v1/lessons")
    suspend fun getLessons(): List<Lesson>?

    @POST("rest/v1/lessons")
    suspend fun putLesson(@Body lesson: Lesson): Response<Unit>

    @DELETE("rest/v1/lessons")
    suspend fun deleteLesson(@Query("id") idQuery: String): Response<Unit>

    // 2. Exams
    @GET("rest/v1/exams")
    suspend fun getExams(): List<Exam>?

    @POST("rest/v1/exams")
    suspend fun putExam(@Body exam: Exam): Response<Unit>

    @DELETE("rest/v1/exams")
    suspend fun deleteExam(@Query("id") idQuery: String): Response<Unit>

    // 3. Submissions
    @GET("rest/v1/quiz_submissions")
    suspend fun getSubmissions(): List<QuizSubmission>?

    @POST("rest/v1/quiz_submissions")
    suspend fun putSubmission(@Body submission: QuizSubmission): Response<Unit>

    // 4. Students Profiles
    @GET("rest/v1/students")
    suspend fun getStudents(): List<StudentProfile>?

    @GET("rest/v1/students")
    suspend fun getStudentByCode(@Query("id") codeQuery: String): List<StudentProfile>?

    @POST("rest/v1/students")
    suspend fun putStudent(@Body profile: StudentProfile): Response<Unit>

    @DELETE("rest/v1/students")
    suspend fun deleteStudent(@Query("id") idQuery: String): Response<Unit>

    // 5. Activation Codes
    @GET("rest/v1/activation_codes")
    suspend fun getActivationCodes(): List<ActivationCode>?

    @POST("rest/v1/activation_codes")
    suspend fun putActivationCode(@Body code: ActivationCode): Response<Unit>
}

object RetrofitClient {
    private const val BASE_URL = "https://wxmzndunjbempfcwkqqb.supabase.co/"
    private const val SUPABASE_KEY = "sb_publishable_NknDg83Ggiq9_7x9qiv9tQ_4RSlVxzw"

    private val moshi = Moshi.Builder()
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Supabase Auth and PostgREST Request Headers Interceptor
    private val headersInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .header("Content-Type", "application/json")
        
        // PostgREST Upsert support: When doing a POST request, use resolution=merge-duplicates to upsert
        if (original.method == "POST") {
            builder.header("Prefer", "resolution=merge-duplicates")
        }
        
        chain.proceed(builder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(headersInterceptor)
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
