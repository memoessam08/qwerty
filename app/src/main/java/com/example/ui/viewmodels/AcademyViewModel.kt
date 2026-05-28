package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AcademyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AcademyRepository(database.academyDao())

    // UI flows from DB
    val allLessons = repository.allLessons
    val allExams = repository.allExams
    val allSubmissions = repository.allSubmissions

    private val _currentRole = MutableStateFlow(Role.STUDENT)
    val currentRole: StateFlow<Role> = _currentRole.asStateFlow()
    
    enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR }
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _studentName = MutableStateFlow("طالب زائر")
    val studentName: StateFlow<String> = _studentName.asStateFlow()

    private val _studentCode = MutableStateFlow("STU-1234")
    val studentCode: StateFlow<String> = _studentCode.asStateFlow()

    private val _selectedGrade = MutableStateFlow("الصف الثالث الثانوي")
    val selectedGrade: StateFlow<String> = _selectedGrade.asStateFlow()

    private val _activeExam = MutableStateFlow<Exam?>(null)
    val activeExam: StateFlow<Exam?> = _activeExam.asStateFlow()

    enum class Role { STUDENT, TEACHER }

    private fun generateRandomStudentCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val randomString = (1..5)
            .map { chars.random() }
            .joinToString("")
        return "STU-$randomString"
    }

    init {
        // Load configurations
        val prefs = application.getSharedPreferences("academy_prefs", android.content.Context.MODE_PRIVATE)
        _studentName.value = prefs.getString("student_name", "أحمد عصام") ?: "أحمد عصام"
        _selectedGrade.value = prefs.getString("selected_grade", "الصف الثالث الثانوي") ?: "الصف الثالث الثانوي"
        
        var code = prefs.getString("student_code", "") ?: ""
        if (code.isBlank()) {
            code = generateRandomStudentCode()
            prefs.edit().putString("student_code", code).apply()
        }
        _studentCode.value = code

        viewModelScope.launch {
            allLessons.first().let { lessons ->
                if (lessons.isEmpty()) {
                    seedDefaultEducationalData()
                }
            }
            syncWithCloud()
        }
    }

    fun setRole(role: Role) {
        _currentRole.value = role
    }

    fun updateStudentProfile(name: String, grade: String, customCode: String? = null) {
        _studentName.value = name.ifBlank { "أحمد عصام" }
        _selectedGrade.value = grade
        
        val finalCode = if (customCode.isNullOrBlank()) {
            _studentCode.value.ifBlank { generateRandomStudentCode() }
        } else {
            customCode.trim().uppercase()
        }
        _studentCode.value = finalCode

        val prefs = getApplication<Application>().getSharedPreferences("academy_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("student_name", _studentName.value)
            .putString("selected_grade", grade)
            .putString("student_code", finalCode)
            .apply()
    }

    fun startExam(exam: Exam) {
        _activeExam.value = exam
    }

    fun finishExam() {
        _activeExam.value = null
    }

    fun saveLesson(lesson: Lesson) {
        val finalLesson = if (lesson.id == 0) {
            lesson.copy(id = (System.currentTimeMillis() % 100000000).toInt())
        } else {
            lesson
        }
        viewModelScope.launch {
            repository.insertLesson(finalLesson)
            try {
                RetrofitClient.apiService.putLesson(finalLesson.id.toString(), finalLesson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleLessonCompleted(lesson: Lesson) {
        viewModelScope.launch {
            repository.updateLesson(lesson.copy(isCompleted = !lesson.isCompleted))
        }
    }

    fun deleteLesson(lessonId: Int) {
        viewModelScope.launch {
            repository.deleteLesson(lessonId)
            try {
                RetrofitClient.apiService.deleteLesson(lessonId.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveExam(exam: Exam) {
        val finalExam = if (exam.id == 0) {
            exam.copy(id = (System.currentTimeMillis() % 100000000).toInt())
        } else {
            exam
        }
        viewModelScope.launch {
            repository.insertExam(finalExam)
            try {
                RetrofitClient.apiService.putExam(finalExam.id.toString(), finalExam)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteExam(examId: Int) {
        viewModelScope.launch {
            repository.deleteExam(examId)
            try {
                RetrofitClient.apiService.deleteExam(examId.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveSubmission(submission: QuizSubmission) {
        val finalSubmission = if (submission.id == 0) {
            submission.copy(id = (System.currentTimeMillis() % 100000000).toInt())
        } else {
            submission
        }
        viewModelScope.launch {
            repository.insertSubmission(finalSubmission)
            try {
                RetrofitClient.apiService.putSubmission(finalSubmission.id.toString(), finalSubmission)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                val dao = database.academyDao()
                
                // 1. Sync Lessons
                val cloudLessons = RetrofitClient.apiService.getLessons()
                if (cloudLessons != null) {
                    val localLessons = dao.getLessonsOnce()
                    for (cloudLesson in cloudLessons.values) {
                        val localMatch = localLessons.find { it.id == cloudLesson.id }
                        if (localMatch != null) {
                            val merged = cloudLesson.copy(
                                isCompleted = localMatch.isCompleted,
                                isUnlocked = localMatch.isUnlocked,
                                watchTimeSeconds = localMatch.watchTimeSeconds,
                                isLocalVideo = localMatch.isLocalVideo
                            )
                            dao.insertLesson(merged)
                        } else {
                            dao.insertLesson(cloudLesson)
                        }
                    }
                }

                // 2. Sync Exams
                val cloudExams = RetrofitClient.apiService.getExams()
                if (cloudExams != null) {
                    for (cloudExam in cloudExams.values) {
                        dao.insertExam(cloudExam)
                    }
                }

                // 3. Sync Submissions
                val cloudSubmissions = RetrofitClient.apiService.getSubmissions()
                if (cloudSubmissions != null) {
                    for (cloudSubmission in cloudSubmissions.values) {
                        dao.insertSubmission(cloudSubmission)
                    }
                }

                _syncStatus.value = SyncStatus.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    // تفعيل الحصة بالكود المدخل من الطالب
    fun unlockLesson(lesson: Lesson, enteredCode: String): Boolean {
        val matches = lesson.activationCode.trim().equals(enteredCode.trim(), ignoreCase = true)
        if (matches) {
            viewModelScope.launch {
                repository.updateLesson(lesson.copy(isUnlocked = true))
            }
            return true
        }
        return false
    }

    // تحديث وتراكم زمن حضور ومشاهدة الطالب للحصة بالثواني
    fun updateLessonWatchTime(lesson: Lesson, additionalSeconds: Int) {
        viewModelScope.launch {
            repository.updateLesson(lesson.copy(
                watchTimeSeconds = lesson.watchTimeSeconds + additionalSeconds
            ))
        }
    }

    private suspend fun seedDefaultEducationalData() {
        val sampleLessons = listOf(
            Lesson(
                id = 101,
                title = "محاضرة التأسيس: مدخل لدراسة التاريخ والوعي التاريخي",
                description = "في هذه المحصة المجانية الأولى، يشرح الأستاذ حسين حسن أهمية علم التاريخ وكيفية تحليل الأحداث التاريخية واستنباط الدروس بطرق التفكير النقدي بعيداً عن الحفظ التلقيني.",
                subject = "التاريخ",
                gradeLevel = "الصف الثالث الثانوي",
                videoId = "https://www.w3.org/2010/05/video/mediaelement.mp4", // Free public test mp4 link
                pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                durationMinutes = 15,
                isCompleted = false,
                activationCode = "", // Free, no code needed
                isUnlocked = true,
                isLocalVideo = false
            ),
            Lesson(
                id = 102,
                title = "الفصل الأول: الحملة الفرنسية وبناء المجتمع المصري المصري الحديث",
                description = "دراسة شاملة ومفصلة تحت إشراف الأستاذ حسين حسن لأسباب مجيء الحملة الفرنسية وتحليل أوجه التباين بين المجتمعين الفرنسي والمصري سياسياً واجتماعياً.",
                subject = "التاريخ",
                gradeLevel = "الصف الثالث الثانوي",
                videoId = "https://www.w3.org/2010/05/video/mediaelement.mp4",
                pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                durationMinutes = 45,
                isCompleted = false,
                activationCode = "HASSAN2026", // Activation code for students to test
                isUnlocked = false,
                isLocalVideo = false
            ),
            Lesson(
                id = 103,
                title = "الحصة الكبرى: صعود محمد علي وتشييد أركان مصر المعاصرة",
                description = "رحلة تاريخية شيقة لمعرفة كيف استطاع محمد علي الانفراد بالسلطة وبناء القوة العسكرية والاقتصادية والتعليمية لمصر بمطابقتها لخرائط الامتحانات الوزارية.",
                subject = "التاريخ",
                gradeLevel = "الصف الثالث الثانوي",
                videoId = "https://www.w3.org/2010/05/video/mediaelement.mp4",
                pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                durationMinutes = 60,
                isCompleted = false,
                activationCode = "HIST500", // Required premium code
                isUnlocked = false,
                isLocalVideo = false
            ),
            Lesson(
                id = 104,
                title = "تأسيس الصف الأول الثانوي: عوامل قيام الحضارات القديمة",
                description = "شرح مفصل لعوامل قيام الحضارة المصرية القديمة وتأثير الموقع الجغرافي ونهر النيل في وجدان الدولة الكلاسيكية، مخصص لطلاب الصف الأول الثانوي.",
                subject = "التاريخ",
                gradeLevel = "الصف الأول الثانوي",
                videoId = "https://www.w3.org/2010/05/video/mediaelement.mp4",
                pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                durationMinutes = 30,
                isCompleted = false,
                activationCode = "HIST99",
                isUnlocked = false,
                isLocalVideo = false
            )
        )
        for (lesson in sampleLessons) {
            repository.insertLesson(lesson)
            try {
                RetrofitClient.apiService.putLesson(lesson.id.toString(), lesson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val exam1 = Exam(
            id = 201,
            title = "الاختبار الشامل للباب الأول (الحملة الفرنسية على مصر والشام)",
            subject = "التاريخ",
            gradeLevel = "الصف الثالث الثانوي",
            durationMinutes = 20,
            totalScore = 40,
            questions = listOf(
                Question(
                    id = 1,
                    text = "ما هو المعيار الجغرافي الأساسي الذي استندت عليه فرنسا لتبرير قرار نابليون باستهداف الدولة المصرية عسكرياً؟",
                    options = listOf(
                        "تسهيل السفر والحج للمواطنين الفرنسيين",
                        "ضرب المصالح التجارية لبريطانيا العظمى وقطع طريقها المؤدي للهند وعرقلة نفوذها",
                        "بناء سد مائي على مجرى نهر النيل كدعم تنموي",
                        "نشر الفلسفة الفرنسية الديكارتية في مدرسة الألسن"
                    ),
                    correctIndex = 1
                ),
                Question(
                    id = 2,
                    text = "نجح نابليون بونابرت في مخاطبة وجدان المصريين عبر منشوره الشهير من خلال التركيز على:",
                    options = listOf(
                        "مجاملة الدولة العثمانية والادعاء بمحاربة المماليك الغرباء لتأسيس حكومة محلية للمصريين",
                        "تهديدهم بإبادة الثروة الحيوانية والزراعية بشكل مباشر",
                        "تقديم هدايا ذهبية لكل شيخ من مشايخ الأزهر الشريف",
                        "دعوتهم لاعتناق الديانة الرومانية الأرثوذكسية"
                    ),
                    correctIndex = 0
                ),
                Question(
                    id = 3,
                    text = "ترتب على موقعة أبي قير البحرية العسكرية قيام بريطانيا بفرض حصار شديد لشواطئ مصر، مما أدى لـ:",
                    options = listOf(
                        "انتعاش حركة التجارة الخارجية لمصر مع دول البحر المتوسط",
                        "حرمان الحملة الفرنسية من الإمدادات الضرورية القادمة من فرنسا وضعف موقف نابليون",
                        "توطيد العلاقات التجارية الفرنسية مع المماليك في الصعيد فوراً",
                        "سقوط الدولة العثمانية العلية بصورة نهائية"
                    ),
                    correctIndex = 1
                ),
                Question(
                    id = 4,
                    text = "أنشأ نابليون 'المجمع العلمي المصري' لنشر العلوم والبحث العلمي، بالإضافة إلى غاية أساسية أخرى وهي:",
                    options = listOf(
                        "تقديم دراسات واستشارات متكاملة للحكومة لربط السياسة بالدراسة والعلم",
                        "تعليم الشعب المصري اللغة والآداب الفرنسية الرومانسية بالكامل وبشكل إلزامي",
                        "إرسال بعثات علمية من الأزهر لباريس لتطوير صناعة الحرير",
                        "تنظيم عروض أوبرا مسرحية وثقافية مستمرة بالصعيد"
                    ),
                    correctIndex = 0
                )
            )
        )

        val exam2 = Exam(
            id = 202,
            title = "الاختبار القصير الثاني على صعود محمد علي وبناء الجيش الحديث",
            subject = "التاريخ",
            gradeLevel = "الصف الثالث الثانوي",
            durationMinutes = 10,
            totalScore = 20,
            questions = listOf(
                Question(
                    id = 1,
                    text = "استطاع محمد علي إعطاء الطابع الوطني والاحترافي للجيش المصري القديم بالاعتماد أولاً في التجنيد الأساسي على:",
                    options = listOf(
                        "المماليك الفارين للصعيد",
                        "الأتراك الأرناؤوط المتمردين",
                        "السودانيين أولاً ثم تدريجياً على الفلاحين والجنود المصريين الوطنيين",
                        "خبراء جلبهم خصيصاً من إنجلترا وروسيا"
                    ),
                    correctIndex = 2
                ),
                Question(
                    id = 2,
                    text = "الوسيلة الأساسية التي اتبعها محمد علي لفرض سيطرته الكاملة على الأسواق وصناعة الدولة وتجارتها الخارجية تمثلت في نظام الـ:",
                    options = listOf(
                        "الاحتكار والدعم المباشر لمدخلات الصناعة وتحديد الأسعار",
                        "الالتزام العثماني القديم وتفويض جباة الضرائب",
                        "الاقتصاد الرأسمالي وحرية الاستيراد غير الكفء",
                        "الإقطاع الزراعي الشامل وتوزيع الأراضي على عائلته فقط"
                    ),
                    correctIndex = 0
                )
            )
        )

        repository.insertExam(exam1)
        repository.insertExam(exam2)
        try {
            RetrofitClient.apiService.putExam(exam1.id.toString(), exam1)
            RetrofitClient.apiService.putExam(exam2.id.toString(), exam2)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sampleSubmissions = listOf(
            QuizSubmission(
                id = 301,
                examId = 201,
                examTitle = "الاختبار الشامل للباب الأول (الحملة الفرنسية على مصر والشام)",
                subject = "التاريخ",
                studentName = "مي عصام الدين الجمال",
                score = 40,
                totalScore = 40,
                correctCount = 4,
                dateSubmitted = System.currentTimeMillis() - 86400000 * 3
            ),
            QuizSubmission(
                id = 302,
                examId = 201,
                examTitle = "الاختبار الشامل للباب الأول (الحملة الفرنسية على مصر والشام)",
                subject = "التاريخ",
                studentName = "أحمد عصام", // We map احمد عصام as our current user profiles
                score = 30,
                totalScore = 40,
                correctCount = 3,
                dateSubmitted = System.currentTimeMillis() - 3600000 * 20
            ),
            QuizSubmission(
                id = 303,
                examId = 202,
                examTitle = "الاختبار القصير الثاني على صعود محمد علي وبناء الجيش الحديث",
                subject = "التاريخ",
                studentName = "عبد الله السيد خليل",
                score = 20,
                totalScore = 20,
                correctCount = 2,
                dateSubmitted = System.currentTimeMillis() - 3600000 * 5
            ),
            QuizSubmission(
                id = 304,
                examId = 202,
                examTitle = "الاختبار القصير الثاني على صعود محمد علي وبناء الجيش الحديث",
                subject = "التاريخ",
                studentName = "مي عصام الدين الجمال",
                score = 10,
                totalScore = 20,
                correctCount = 1,
                dateSubmitted = System.currentTimeMillis() - 3600000 * 8
            )
        )
        for (submission in sampleSubmissions) {
            repository.insertSubmission(submission)
            try {
                RetrofitClient.apiService.putSubmission(submission.id.toString(), submission)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class AcademyViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AcademyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AcademyViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
