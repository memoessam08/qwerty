package com.example.data

import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

// 1. Question model helper
data class Question(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

// 2. Type converter for Room to handle list serialization using moshi
class QuestionListConverter {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, Question::class.java)
    private val adapter = moshi.adapter<List<Question>>(type)

    @TypeConverter
    fun fromString(value: String?): List<Question> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toString(list: List<Question>?): String {
        if (list == null) return "[]"
        return try {
            adapter.toJson(list) ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }
}

// 3. Lesson Entity (الحصة الدراسية)
@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,               // عنوان الدرس
    val description: String,         // وصف الدرس واهدافه
    val subject: String,             // المادة (تاريخ، فيزياء، الخ)
    val gradeLevel: String,          // السنة الدراسية (الصف الأول، الثاني، الثالث الثانوي)
    val videoId: String,             // يوتيوب فيديو ايدي او رابط فيديو الحصة محلياً
    val pdfUrl: String,              // ملفات الشرح والمذكرات المرفقة
    val durationMinutes: Int,        // مدة الحصة بالدقائق
    val isCompleted: Boolean = false,// حالة الانتهاء للطالب
    val datePublished: Long = System.currentTimeMillis(),
    val activationCode: String = "", // كود التفعيل المطلوب لفتح الحصة
    val isUnlocked: Boolean = false, // هل فُتحت الحصة من قبل الطالب بإدخال الكود?
    val watchTimeSeconds: Int = 0,   // الزمن بالثواني الذي قضاه الطالب فعلياً داخل الدرس
    val isGoogleDrive: Boolean = false // هل الفيديو من جوجل درايف?
)

// 3.1 StudentProfile Entity (ملفات الطلاب)
@Entity(tableName = "students")
data class StudentProfile(
    @PrimaryKey val id: String, // الكود الشخصي للطالب (مثلا STU-XXXXX)
    val name: String,
    val gradeLevel: String,
    val joinedAt: Long = System.currentTimeMillis()
)

// 3.2 ActivationCode Entity (أكواد تفعيل الحصص المنتجة)
@Entity(tableName = "activation_codes")
data class ActivationCode(
    @PrimaryKey val code: String, // كود التفعيل المولد
    val lessonId: Int? = null,    // معرف الدرس المرتبط به (إن وجد)
    val createdAt: Long = System.currentTimeMillis(),
    val isUsed: Boolean = false,
    val usedBy: String = ""       // اسم أو كود الطالب الذي استخدمه
)

// 4. Exam Entity (الامتحانات والاختبارات)
@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,               // عنوان الامتحان
    val subject: String,             // المادة
    val gradeLevel: String,          // السنة الدراسية للطلاب المستهدفين
    val durationMinutes: Int,        // مدة الامتحان بالدقائق
    val totalScore: Int,             // الدرجة الإجمالية
    val questions: List<Question>,   // قائمة الأسئلة (Moshi-serialized)
    val datePublished: Long = System.currentTimeMillis()
)

// 5. QuizSubmission Entity (درجات ومتابعة الطلاب)
@Entity(tableName = "quiz_submissions")
data class QuizSubmission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,                 // رقم الامتحان
    val examTitle: String,           // عنوان الامتحان
    val subject: String,             // المادة
    val studentName: String,         // اسم الطالب المسجل
    val score: Int,                  // الدرجة التي نالها
    val totalScore: Int,             // الدرجة الكلية
    val correctCount: Int,           // عدد الأجوبة الصحيحة
    val dateSubmitted: Long = System.currentTimeMillis()
)

// 6. DAO
@Dao
interface AcademyDao {
    // Lessons
    @Query("SELECT * FROM lessons")
    suspend fun getLessonsOnce(): List<Lesson>

    @Query("SELECT * FROM lessons ORDER BY datePublished DESC")
    fun getAllLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE gradeLevel = :gradeLevel ORDER BY datePublished DESC")
    fun getLessonsByGrade(gradeLevel: String): Flow<List<Lesson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson)

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Query("DELETE FROM lessons WHERE id = :lessonId")
    suspend fun deleteLesson(lessonId: Int)

    // Exams
    @Query("SELECT * FROM exams")
    suspend fun getExamsOnce(): List<Exam>

    @Query("SELECT * FROM exams ORDER BY datePublished DESC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE gradeLevel = :gradeLevel ORDER BY datePublished DESC")
    fun getExamsByGrade(gradeLevel: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE id = :examId LIMIT 1")
    suspend fun getExamById(examId: Int): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :examId")
    suspend fun deleteExam(examId: Int)

    // Submissions
    @Query("SELECT * FROM quiz_submissions")
    suspend fun getSubmissionsOnce(): List<QuizSubmission>

    @Query("SELECT * FROM quiz_submissions ORDER BY dateSubmitted DESC")
    fun getAllSubmissions(): Flow<List<QuizSubmission>>

    @Query("SELECT * FROM quiz_submissions WHERE studentName = :studentName ORDER BY dateSubmitted DESC")
    fun getSubmissionsByStudent(studentName: String): Flow<List<QuizSubmission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: QuizSubmission)

    // Students / Profiles
    @Query("SELECT * FROM students")
    suspend fun getStudentsOnce(): List<StudentProfile>

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(profile: StudentProfile)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: String)

    // Activation Codes
    @Query("SELECT * FROM activation_codes")
    suspend fun getActivationCodesOnce(): List<ActivationCode>

    @Query("SELECT * FROM activation_codes ORDER BY createdAt DESC")
    fun getAllActivationCodes(): Flow<List<ActivationCode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivationCode(code: ActivationCode)
}

// 7. Database wrapper
@Database(
    entities = [Lesson::class, Exam::class, QuizSubmission::class, StudentProfile::class, ActivationCode::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(QuestionListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun academyDao(): AcademyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "academy_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
