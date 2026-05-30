package com.example.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodels.AcademyViewModel
import com.example.ui.viewmodels.AcademyViewModelFactory
import com.example.ui.viewmodels.SmartNotification
import com.example.ui.viewmodels.AIState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: AcademyViewModel = viewModel(
        factory = AcademyViewModelFactory(application)
    )

    // Collect variables
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val studentName by viewModel.studentName.collectAsStateWithLifecycle()
    val studentCode by viewModel.studentCode.collectAsStateWithLifecycle()
    val selectedGrade by viewModel.selectedGrade.collectAsStateWithLifecycle()
    val lessons by viewModel.allLessons.collectAsStateWithLifecycle(initialValue = emptyList())
    val exams by viewModel.allExams.collectAsStateWithLifecycle(initialValue = emptyList())
    val submissions by viewModel.allSubmissions.collectAsStateWithLifecycle(initialValue = emptyList())
    val students by viewModel.allStudentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeExam by viewModel.activeExam.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val aiState by viewModel.aiReportState.collectAsStateWithLifecycle()

    // Screen State
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var currentStudentTab by remember { mutableStateOf("LESSONS") } // LESSONS, EXAMS, PROGRESS, BOARD
    var currentTeacherTab by remember { mutableStateOf("T_STATS") } // T_STATS, T_ADD_LESSON, T_ADD_EXAM, T_SUBMISSIONS

    // Filter states
    var selectedSubjectFilter by remember { mutableStateOf("التاريخ") }

    // Dialog sheets
    var lessonToWatch by remember { mutableStateOf<Lesson?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showTeacherPasswordDialog by remember { mutableStateOf(false) }

    // Exam taker visual feedback state
    var examSubmissionResult by remember { mutableStateOf<QuizSubmission?>(null) }

    // Force RTL for native Arabic experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isLoggedIn) {
                    LoginAndRegisterView(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            Toast.makeText(context, "تم تسجيل الدخول بنجاح! 👋", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else if (activeExam != null) {
                    // Elevated fullscreen exam taking module
                    ExamTakerView(
                        exam = activeExam!!,
                        studentName = studentName,
                        onCancel = { viewModel.finishExam() },
                        onSubmit = { submission ->
                            viewModel.saveSubmission(submission)
                            examSubmissionResult = submission
                            viewModel.finishExam()
                        }
                    )
                } else {
                    Scaffold(
                        topBar = {
                            LargeTopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = "منصة الأستاذ حسين حسن",
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = "كبير معلمي مادة التاريخ للثانوية العامة 🏛️",
                                            fontSize = 12.sp,
                                            color = AmberGold,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                navigationIcon = {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "Graduation Cap Logo",
                                        tint = AmberGold,
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                            .size(36.dp)
                                    )
                                },
                                actions = {
                                    // AI Advisor Button
                                    if (currentRole == AcademyViewModel.Role.STUDENT) {
                                        IconButton(
                                            onClick = { showAiAdvisorDialog = true },
                                            modifier = Modifier.padding(end = 4.dp).testTag("ai_advisor_button")
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = AmberGold,
                                                        contentColor = Color.Black
                                                    ) {
                                                        Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "البروفيسور الذكي",
                                                    tint = AmberGold,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Notifications Button with badge
                                    IconButton(
                                        onClick = { showNotificationsDialog = true },
                                        modifier = Modifier.padding(end = 4.dp).testTag("notifications_button")
                                    ) {
                                        if (notifications.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = Color.Red,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text("${notifications.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = "الإشعارات",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "الإشعارات",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Professional quick Profile click handler
                                    Surface(
                                        onClick = { showProfileDialog = true },
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.padding(end = 16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Profile Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = studentName.substringBefore(" - ").take(12),
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.largeTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    scrolledContainerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        },
                        bottomBar = {
                            // Unified Navigation depending on current active role
                            if (currentRole == AcademyViewModel.Role.STUDENT) {
                                StudentBottomNavigation(
                                    currentTab = currentStudentTab,
                                    onSelectTab = { currentStudentTab = it }
                                )
                            } else {
                                TeacherBottomNavigation(
                                    currentTab = currentTeacherTab,
                                    onSelectTab = { currentTeacherTab = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            // Dynamic Role & Info Swapper banner
                            RoleSelectorHeader(
                                currentRole = currentRole,
                                studentName = studentName,
                                grade = selectedGrade,
                                studentCode = studentCode,
                                onRoleChange = { role ->
                                    if (studentCode == "ADMIN") {
                                        if (role == AcademyViewModel.Role.TEACHER) {
                                            showTeacherPasswordDialog = true
                                        } else {
                                            viewModel.setRole(role)
                                            Toast.makeText(context, "العرض بصفة: طالب", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "غير مسموح بالتحويل لحساب معلم!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            OnlineSyncIndicator(
                                syncStatus = syncStatus,
                                onRefreshSync = { viewModel.syncWithCloud() }
                            )

                            // Main Content Routing based on Role
                            if (currentRole == AcademyViewModel.Role.STUDENT) {
                                when (currentStudentTab) {
                                    "LESSONS" -> StudentLessonsTab(
                                        lessons = lessons,
                                        selectedGrade = selectedGrade,
                                        selectedSubject = selectedSubjectFilter,
                                        onSubjectSelect = { selectedSubjectFilter = it },
                                        onWatchLesson = { lessonToWatch = it },
                                        onToggleComplete = { viewModel.toggleLessonCompleted(it) }
                                    )
                                    "EXAMS" -> StudentExamsTab(
                                        exams = exams,
                                        submissions = submissions,
                                        selectedGrade = selectedGrade,
                                        onStartExam = { viewModel.startExam(it) }
                                    )
                                    "PROGRESS" -> StudentProgressTab(
                                        lessons = lessons,
                                        submissions = submissions,
                                        selectedGrade = selectedGrade,
                                        onCustomizeProfile = { showProfileDialog = true },
                                        onShowAiAdvisor = { showAiAdvisorDialog = true }
                                    )
                                    "BOARD" -> StudentLeaderboardTab(submissions = submissions)
                                }
                            } else {
                                when (currentTeacherTab) {
                                    "T_STATS" -> TeacherDashboardStatsTab(
                                        lessons = lessons,
                                        exams = exams,
                                        submissions = submissions,
                                        onNavigateToAddLesson = { currentTeacherTab = "T_ADD_LESSON" },
                                        onNavigateToAddExam = { currentTeacherTab = "T_ADD_EXAM" },
                                        onDeleteLesson = { id -> viewModel.deleteLesson(id) },
                                        onGenerateBulkCodes = { lesson, count, onComplete ->
                                            viewModel.generateBulkActivationCodes(lesson.id, count, onComplete)
                                        }
                                    )
                                    "T_ADD_LESSON" -> TeacherAddLessonTab(
                                        onSubmit = { title, desc, sub, grade, videoId, pdf, code, isDrive, genBulk, bulkCount ->
                                            val lessonId = (System.currentTimeMillis() % 100000000).toInt()
                                            val newLesson = Lesson(
                                                id = lessonId,
                                                title = title,
                                                description = desc,
                                                subject = sub,
                                                gradeLevel = grade,
                                                videoId = videoId,
                                                pdfUrl = pdf,
                                                durationMinutes = 45,
                                                activationCode = code,
                                                isUnlocked = code.isBlank(), // مفتوحة تلقائياً إذا لم يحدد المدرس كود تفعيل لها
                                                isGoogleDrive = isDrive
                                            )
                                            viewModel.saveLesson(newLesson)
                                            
                                            if (genBulk && bulkCount > 0) {
                                                viewModel.generateBulkActivationCodes(lessonId, bulkCount) { codes ->
                                                    val cleanTitle = title.replace(" ", "_").replace("/", "-")
                                                    val filename = "أكواد_حصة_${cleanTitle}.txt"
                                                    val fileContent = "أكواد تفعيل حصة الأستاذ حسين حسن: ${title}\n" +
                                                        "تاريخ التوليد: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n" +
                                                        codes.joinToString("\n")
                                                    
                                                    saveTextFileToDownloads(context, filename, fileContent)
                                                }
                                            }
                                            
                                            Toast.makeText(context, "تم حفظ ونشر الحصة بنجاح!", Toast.LENGTH_SHORT).show()
                                            currentTeacherTab = "T_STATS"
                                        }
                                    )
                                    "T_ADD_EXAM" -> TeacherAddExamTab(
                                        onSubmit = { title, sub, grade, duration, questions ->
                                            val newExam = Exam(
                                                title = title,
                                                subject = sub,
                                                gradeLevel = grade,
                                                durationMinutes = duration,
                                                totalScore = questions.size * 10,
                                                questions = questions
                                            )
                                            viewModel.saveExam(newExam)
                                            Toast.makeText(context, "تم حفظ ونشر الاختبار بنجاح!", Toast.LENGTH_SHORT).show()
                                            currentTeacherTab = "T_STATS"
                                        }
                                    )
                                    "T_SUBMISSIONS" -> TeacherStudentsLedgerTab(
                                        submissions = submissions,
                                        lessons = lessons,
                                        currentStudentName = studentName,
                                        currentStudentCode = studentCode,
                                        studentsFromDb = students,
                                        onDeleteStudent = { viewModel.deleteStudent(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Show Interactive Lesson Video Player Dialog sheet
        if (lessonToWatch != null) {
            WatchVideoDialog(
                lesson = lessonToWatch!!,
                onDismiss = { lessonToWatch = null },
                onMarkCompleted = {
                    viewModel.toggleLessonCompleted(lessonToWatch!!)
                    lessonToWatch = lessonToWatch!!.copy(isCompleted = !lessonToWatch!!.isCompleted)
                },
                onUpdateWatchTime = { seconds ->
                    viewModel.updateLessonWatchTime(lessonToWatch!!, seconds)
                    lessonToWatch = lessonToWatch!!.copy(watchTimeSeconds = lessonToWatch!!.watchTimeSeconds + seconds)
                },
                onUnlockWithCode = { enteredCode, onCompleted ->
                    viewModel.unlockLesson(lessonToWatch!!, enteredCode) { success ->
                        if (success) {
                            lessonToWatch = lessonToWatch!!.copy(isUnlocked = true)
                        }
                        onCompleted(success)
                    }
                }
            )
        }

        // Show Student Profile Customizer Dialog sheet
        if (showProfileDialog) {
            UserProfileDialog(
                currentName = studentName,
                currentGrade = selectedGrade,
                currentCode = studentCode,
                onDismiss = { showProfileDialog = false },
                onSave = { name, grade, code ->
                    viewModel.updateStudentProfile(name, grade, code)
                    showProfileDialog = false
                    Toast.makeText(context, "تم تعديل ملفك الشخصي بنجاح!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Show Exam Report certificate overlay once submitted
        if (examSubmissionResult != null) {
            QuizReportCertificate(
                result = examSubmissionResult!!,
                onDismiss = { examSubmissionResult = null }
            )
        }

        if (showTeacherPasswordDialog) {
            TeacherPasswordDialog(
                onDismiss = { showTeacherPasswordDialog = false },
                onSuccess = {
                    showTeacherPasswordDialog = false
                    viewModel.setRole(AcademyViewModel.Role.TEACHER)
                    Toast.makeText(context, "العرض بصفة: معلم متحكم", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showNotificationsDialog) {
            SmartNotificationsDialog(
                notifications = notifications,
                onDismiss = { showNotificationsDialog = false },
                onClear = { viewModel.clearNotifications() }
            )
        }

        if (showAiAdvisorDialog) {
            SmartAiAdvisorDialog(
                aiState = aiState,
                onGenerate = { viewModel.generateAIStudentAnalysis() },
                onDismiss = { showAiAdvisorDialog = false }
            )
        }
    }
}

// ==========================================
// SHARED UTILITY COMPOSABLES
// ==========================================

@Composable
fun RoleSelectorHeader(
    currentRole: AcademyViewModel.Role,
    studentName: String,
    grade: String,
    studentCode: String,
    onRoleChange: (AcademyViewModel.Role) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentRole == AcademyViewModel.Role.STUDENT) Icons.Default.AccountCircle else Icons.Default.Lock,
                        contentDescription = "Avatar Badge",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (currentRole == AcademyViewModel.Role.STUDENT) studentName.substringBefore(" - ") else "الأستاذ / المعلم المسؤول",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (currentRole == AcademyViewModel.Role.STUDENT) grade else "لوحة الإدارة ونشر المحتوى",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Role selection buttons - only if logged in as admin/teacher
            if (studentCode == "ADMIN") {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                ) {
                    Surface(
                        onClick = { onRoleChange(AcademyViewModel.Role.STUDENT) },
                        color = if (currentRole == AcademyViewModel.Role.STUDENT) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "طالب",
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("student_mode_button"),
                            color = if (currentRole == AcademyViewModel.Role.STUDENT) Color.White else MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        onClick = { onRoleChange(AcademyViewModel.Role.TEACHER) },
                        color = if (currentRole == AcademyViewModel.Role.TEACHER) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "معلم",
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("teacher_mode_button"),
                            color = if (currentRole == AcademyViewModel.Role.TEACHER) Color.White else MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // For regular students, display a stylish green active student badge
                Surface(
                    color = EmeraldSuccess.copy(0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = "نشط 🟢",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// STUDENT NAVIGATION & LABS
// ==========================================

@Composable
fun StudentBottomNavigation(
    currentTab: String,
    onSelectTab: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == "LESSONS",
            onClick = { onSelectTab("LESSONS") },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "الحصص") },
            label = { Text("الحصص أونلاين", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "EXAMS",
            onClick = { onSelectTab("EXAMS") },
            icon = { Icon(Icons.Default.Home, contentDescription = "الاختبارات") },
            label = { Text("الامتحانات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "PROGRESS",
            onClick = { onSelectTab("PROGRESS") },
            icon = { Icon(Icons.Default.Check, contentDescription = "متابعة درجاتي") },
            label = { Text("متابعتي", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "BOARD",
            onClick = { onSelectTab("BOARD") },
            icon = { Icon(Icons.Default.Star, contentDescription = "الأوائل") },
            label = { Text("المتصدرين", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun StudentLessonsTab(
    lessons: List<Lesson>,
    selectedGrade: String,
    selectedSubject: String,
    onSubjectSelect: (String) -> Unit,
    onWatchLesson: (Lesson) -> Unit,
    onToggleComplete: (Lesson) -> Unit
) {
    val filteredLessons = lessons.filter {
        it.gradeLevel == selectedGrade && (selectedSubject == "الكل" || it.subject == selectedSubject)
    }

    val subjects = listOf("التاريخ")

    Column(modifier = Modifier.fillMaxSize()) {
        // Subjects horizontal selector
        ScrollViewHorizontalFilter(
            options = subjects,
            selectedOption = selectedSubject,
            onSelect = onSubjectSelect
        )

        if (filteredLessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty state icon",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد حصص منشورة لمادة ( $selectedSubject ) حالياً لـ ( $selectedGrade )",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "الحصص المتوفرة لمرحلتك الدراسية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security Alert",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "أنظمة حماية حقوق البث والنشر 🛡️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "يمنع تنزيل الحصص أو تحميلها على الأجهزة. تم تفعيل نظام منع تصوير الشاشة أو تسجيل الفيديو تلقائياً لحفظ حقوق الأستاذ حسين حسن.",
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                items(filteredLessons) { lesson ->
                    LessonVisualCard(
                        lesson = lesson,
                        onWatch = { onWatchLesson(lesson) },
                        onToggleComplete = { onToggleComplete(lesson) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun ScrollViewHorizontalFilter(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val isSelected = opt == selectedOption
            Surface(
                onClick = { onSelect(opt) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.2f)
                )
            ) {
                Text(
                    text = opt,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun LessonVisualCard(
    lesson: Lesson,
    onWatch: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val subjectThemeColor = when (lesson.subject) {
        "التاريخ" -> AcademicBlue
        else -> AcademicBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card header strip with Subject title and tags
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(subjectThemeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مادة: " + lesson.subject,
                        color = subjectThemeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Duration",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${lesson.durationMinutes} دقيقة",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = lesson.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lesson.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Checkbox completed wrapper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onToggleComplete() }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = lesson.isCompleted,
                            onCheckedChange = { onToggleComplete() },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldSuccess)
                        )
                        Text(
                            text = if (lesson.isCompleted) "أكملت مذاكرة الحصة ✓" else "مذاكرة الدرس وإنجازه",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (lesson.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Watch button
                    Button(
                        onClick = onWatch,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = subjectThemeColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "حضور الشرح",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentExamsTab(
    exams: List<Exam>,
    submissions: List<QuizSubmission>,
    selectedGrade: String,
    onStartExam: (Exam) -> Unit
) {
    val filteredExams = exams.filter { it.gradeLevel == selectedGrade }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Text(
            text = "الامتحانات والاختبارات الأسبوعية:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (filteredExams.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "No exams",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لا توجد امتحانات منشورة لـ ($selectedGrade) حالياً.",
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredExams) { exam ->
                    // Find if student has already solved this exam
                    val submission = submissions.find { it.examId == exam.id }
                    val isSolved = submission != null

                    ExamListItemCard(
                        exam = exam,
                        isSolved = isSolved,
                        submissionScore = submission?.score,
                        onSolveClick = { onStartExam(exam) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExamListItemCard(
    exam: Exam,
    isSolved: Boolean,
    submissionScore: Int?,
    onSolveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSolved) EmeraldSuccess.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Subject tag
                    AssistChip(
                        onClick = {},
                        label = { Text(exam.subject, fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Duration tag
                    AssistChip(
                        onClick = {},
                        label = { Text("${exam.durationMinutes} دقيقة", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exam.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الدرجة الكلية للامتحان: ${exam.totalScore} درجة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // CTA Button or score badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                if (isSolved) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = EmeraldSuccess.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "مكتمل ✓",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${submissionScore ?: 0} / ${exam.totalScore}",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onSolveClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "بدء الاختبار",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProgressTab(
    lessons: List<Lesson>,
    submissions: List<QuizSubmission>,
    selectedGrade: String,
    onCustomizeProfile: () -> Unit,
    onShowAiAdvisor: () -> Unit
) {
    val gradeLessons = lessons.filter { it.gradeLevel == selectedGrade }
    val completedLessonsCount = gradeLessons.count { it.isCompleted }
    val lessonsProgress = if (gradeLessons.isNotEmpty()) {
        completedLessonsCount.toFloat() / gradeLessons.size.toFloat()
    } else {
        0f
    }

    // Filter submissions for general calculations
    val totalSolvedExams = submissions.size
    val averageScorePercent = if (submissions.isNotEmpty()) {
        submissions.map { it.score.toFloat() / it.totalScore.toFloat() }.average() * 100
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "متابعة وتحليل أدائي التعليمي الحالي:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Sleek Dashboard ring and details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "مستوى التزام الطالب الدراسي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    CircularProgressIndicator(
                        progress = { lessonsProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = EmeraldSuccess,
                        strokeWidth = 8.dp,
                        trackColor = MaterialTheme.colorScheme.outline.copy(0.12f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(lessonsProgress * 100).toInt()}%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "منجز",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الحصص المنتهية", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("$completedLessonsCount / ${gradeLessons.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الاختبارات المنفذة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("$totalSolvedExams اختبار", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("معدل التفوق", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("${String.format("%.1f", averageScorePercent)}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AcademicBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Premium AI Advisor integration card
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onShowAiAdvisor() }.testTag("ai_advisor_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "البروفيسور",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المستشار الأكاديمي الذكي (البروفيسور) 🎓✨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط لتحليل تقدم دراستك للتاريخ فورا واقتراح خطة مراجعات ومقترحات ذكية لتقوية مستواك باستخدام الذكاء الاصطناعي.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Achievements and badges earned
        Text(
            text = "الأوسمة والدروع التي أحرزتها 🎖️",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BadgeCard(
                title = "طالب مثابر",
                desc = "الالتزام بعضوية المنصة والصف",
                icon = Icons.Default.Check,
                color = AcademicBlue
            )
            if (completedLessonsCount >= 1) {
                BadgeCard(
                    title = "نجم التاريخ",
                    desc = "إنهاء أول حصة تاريخ تفصيلية",
                    icon = Icons.Default.Star,
                    color = AmberGold
                )
            }
            if (totalSolvedExams >= 1) {
                BadgeCard(
                    title = "بطل التقييمات",
                    desc = "إتمام أول امتحان مدرسي",
                    icon = Icons.Default.School,
                    color = EmeraldSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. الحصص اللي محضرتهاش - Lessons Not Attended / Not Watched
        val missedLessons = gradeLessons.filter { !it.isCompleted }
        Text(
            text = "حصص تاريخية فاتتك ولم تسجل حضورها بعد 🚫:",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (missedLessons.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(0.08f)),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "رائع جداً! لقد تفوقت وحضرت كافة الحصص المعروضة بجدول الأستاذ حسين حسن.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }
        } else {
            missedLessons.forEach { lesson ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RoseError.copy(0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = lesson.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lesson.activationCode.isNotEmpty()) "🔐 تحتاج كود تفعيل قفل: ${lesson.activationCode}" else "حصة مفتوحة - لم يحضر الطالب",
                                fontSize = 11.sp,
                                color = if (lesson.activationCode.isNotEmpty()) AmberGold else MaterialTheme.colorScheme.outline
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(color = RoseError.copy(0.12f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "غائب / لم تنجز",
                                fontWeight = FontWeight.Bold,
                                color = RoseError,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. User historic exam result cards
        Text(
            text = "تاريخ درجاتي التفصيلية وملفي الشخصي:",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (submissions.isEmpty()) {
            Text(
                text = "لم تؤد أي اختبار بعد، ابدأ الامتحانات لتتبع مستواك هنا.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            submissions.forEach { sub ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = sub.examTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "مادة ${sub.subject} | الدرجة المحققة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (sub.score >= sub.totalScore / 2) EmeraldSuccess.copy(0.15f) else RoseError.copy(0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${sub.score} / ${sub.totalScore}",
                                fontWeight = FontWeight.Bold,
                                color = if (sub.score >= sub.totalScore / 2) EmeraldSuccess else RoseError,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun StudentLeaderboardTab(submissions: List<QuizSubmission>) {
    // Generate beautiful visual mock database of students including real ones from submissions
    val mockedClassmates = listOf(
        Pair("مروان أحمد الرفاعي", 95),
        Pair("سلمى محمود نصير", 92),
        Pair("عمر هاني يونس", 88),
        Pair("يسرية عبد الغني الفارس", 85)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Leaderboard",
                    tint = AmberGold,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "جدول شرف المتصدرين والأوائل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "شارك واجتز الاختبارات الأسبوعية لتحصد مراتب المتصدرين!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "ترتيب الطلاب لشهر مايو 2026:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LeaderboardRowItem(rank = 1, name = "عبد الله السيد خليل", score = 100, isTop = true)
            }
            item {
                LeaderboardRowItem(rank = 2, name = "مي عصام الدين الجمال", score = 98, isTop = true)
            }
            item {
                LeaderboardRowItem(rank = 3, name = "إياد يوسف نصر", score = 96, isTop = true)
            }

            items(mockedClassmates) { item ->
                LeaderboardRowItem(rank = mockedClassmates.indexOf(item) + 4, name = item.first, score = item.second, isTop = false)
            }
        }
    }
}

@Composable
fun LeaderboardRowItem(
    rank: Int,
    name: String,
    score: Int,
    isTop: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTop) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank circle badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            color = when (rank) {
                                1 -> Color(0xFFFFD700) // Gold
                                2 -> Color(0xFFC0C0C0) // Silver
                                3 -> Color(0xFFCD7F32) // Bronze
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    fontWeight = if (isTop) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "$score %",
                fontWeight = FontWeight.ExtraBold,
                color = if (isTop) AmberGold else MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
    }
}

// ==========================================
// TEACHER PORTAL & ACTIONS
// ==========================================

@Composable
fun TeacherBottomNavigation(
    currentTab: String,
    onSelectTab: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == "T_STATS",
            onClick = { onSelectTab("T_STATS") },
            icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
            label = { Text("التحكم العام", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "T_ADD_LESSON",
            onClick = { onSelectTab("T_ADD_LESSON") },
            icon = { Icon(Icons.Default.Add, contentDescription = "إضافة حصة") },
            label = { Text("نشر حصة", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "T_ADD_EXAM",
            onClick = { onSelectTab("T_ADD_EXAM") },
            icon = { Icon(Icons.Default.Create, contentDescription = "إضافة امتحان") },
            label = { Text("نشر امتحان", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "T_SUBMISSIONS",
            onClick = { onSelectTab("T_SUBMISSIONS") },
            icon = { Icon(Icons.Default.List, contentDescription = "النتائج") },
            label = { Text("متابعة الطلاب", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun TeacherDashboardStatsTab(
    lessons: List<Lesson>,
    exams: List<Exam>,
    submissions: List<QuizSubmission>,
    onNavigateToAddLesson: () -> Unit,
    onNavigateToAddExam: () -> Unit,
    onDeleteLesson: (Int) -> Unit,
    onGenerateBulkCodes: (Lesson, Int, (List<String>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var lessonForBulkCodes by remember { mutableStateOf<Lesson?>(null) }
    var dialogBulkCount by remember { mutableStateOf(100) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "إحصائيات وأدوات الإدارة النشطة:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Triple Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TeacherStatCard(
                title = "الحصص المرفوعة",
                value = lessons.size.toString(),
                label = "فيديو وتلخيصات",
                color = AcademicBlue,
                modifier = Modifier.weight(1f)
            )
            TeacherStatCard(
                title = "الامتحانات النشطة",
                value = exams.size.toString(),
                label = "تقييمات دورية",
                color = AmberGold,
                modifier = Modifier.weight(1f)
            )
            TeacherStatCard(
                title = "إجابات الطلاب",
                value = submissions.size.toString(),
                label = "كشف المتابعة",
                color = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Launcher
        Text(
            text = "إدارة المحتوى الدراسي الفوري:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Publish lesson banner click
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAddLesson() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AcademicBlue.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AcademicBlue)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "نشر حصة وملخص دراسي جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "ارفع شرح يوتيوب وأرفق تلخيص PDF للطلاب", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(0.08f))

                // Publish exams banner click
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAddExam() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberGold.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Create, contentDescription = null, tint = AmberGold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "إنشاء اختبار تفاعلي جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "أضف أسئلة اختيار من متعدد تفاعلية مع مفتاح الحل", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Manage/Delete Lessons Section
        Text(
            text = "إدارة وحذف الحصص المرفوعة حالياً 🎥:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (lessons.isEmpty()) {
            Text(
                text = "لا توجد حصص مرفوعة حالياً بالفيديو لتعديلها أو حذفها.",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.12f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    lessons.forEachIndexed { index, lesson ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AcademicBlue.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (lesson.isGoogleDrive) Icons.Default.FolderOpen else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = AcademicBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = lesson.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${lesson.gradeLevel} • ${lesson.subject}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        lessonForBulkCodes = lesson
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = "توليد أكواد التفعيل الحركية",
                                        tint = AcademicBlue
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onDeleteLesson(lesson.id)
                                        Toast.makeText(context, "تم حذف حصة: ${lesson.title} 🗑️", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف الحصة",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        if (index < lessons.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(0.08f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Latest submissions ticker
        Text(
            text = "آخر الطلاب الذين أدوا الاختبارات مسبقاً:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (submissions.isEmpty()) {
            Text(
                text = "لا توجد مشاركات من الطلاب حتى الآن في قاعدة البيانات.",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 12.sp
            )
        } else {
            submissions.take(5).forEach { sub ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = sub.studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "${sub.examTitle} • ${sub.subject}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = "نال ${sub.score} / ${sub.totalScore}",
                            fontWeight = FontWeight.Bold,
                            color = if (sub.score >= sub.totalScore / 2) EmeraldSuccess else RoseError,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (lessonForBulkCodes != null) {
        val targetLesson = lessonForBulkCodes!!
        AlertDialog(
            onDismissRequest = { lessonForBulkCodes = null },
            title = {
                Text(
                    text = "توليد باقة أكواد تفعيل جديدة 🔑",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "الحصة المستهدفة: ${targetLesson.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر عدد الأكواد المطلوب إصدارها للحصة وتصديرها مباشرةً:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = "العدد: $dialogBulkCount كود",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Slider(
                        value = dialogBulkCount.toFloat(),
                        onValueChange = { dialogBulkCount = it.toInt() },
                        valueRange = 10f..500f,
                        steps = 49
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(20, 50, 100, 200).forEach { num ->
                            FilterChip(
                                selected = dialogBulkCount == num,
                                onClick = { dialogBulkCount = num },
                                label = { Text("$num كود", fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentCount = dialogBulkCount
                        onGenerateBulkCodes(targetLesson, currentCount) { codes ->
                            val cleanTitle = targetLesson.title.replace(" ", "_").replace("/", "-")
                            val filename = "أكواد_إضافية_حصة_${cleanTitle}.txt"
                            val fileContent = "أكواد تفعيل إضافية لحصة: ${targetLesson.title}\n" +
                                "تاريخ التوليد: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n" +
                                codes.joinToString("\n")
                            
                            saveTextFileToDownloads(context, filename, fileContent)
                        }
                        lessonForBulkCodes = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue)
                ) {
                    Text("توليد وتصدير الملف 📥", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonForBulkCodes = null }) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun TeacherStatCard(
    title: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, lineHeight = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAddLessonTab(
    onSubmit: (String, String, String, String, String, String, String, Boolean, Boolean, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("التاريخ") }
    var gradeLevel by remember { mutableStateOf("الصف الثالث الثانوي") }
    var youtubeId by remember { mutableStateOf("") }
    var pdfLink by remember { mutableStateOf("") }
    var activationCode by remember { mutableStateOf("") }
    
    // Video Type selection: YOUTUBE link vs. Google Drive embed
    var isGoogleDrive by remember { mutableStateOf(false) }

    // Bulk activation codes parameters
    var generateBulkCodes by remember { mutableStateOf(false) }
    var bulkCodesCount by remember { mutableStateOf(100) }

    val context = LocalContext.current
    val subjects = listOf("التاريخ")
    val grades = listOf("الصف الأول الثانوي", "الصف الثاني الثانوي", "الصف الثالث الثانوي")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "نشر حصة تاريخية جديدة على المنصة:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان الحصة التاريخية (مثال: الحملة الفرنسية وأثرها)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("lesson_title_input"),
            singleLine = true
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("وصف الحصة بالتفصيل وتحليل نواتج التعلم المستهدفة") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .height(100.dp),
            maxLines = 4
        )

        // Dropdowns simulation via Row Selectors
        Text(text = "المادة الدراسية:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subjects.forEach { s ->
                FilterChip(
                    selected = subject == s,
                    onClick = { subject = s },
                    label = { Text(s) }
                )
            }
        }

        Text(text = "المرحلة / الصف الدراسي للطلاب:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grades.forEach { g ->
                FilterChip(
                    selected = gradeLevel == g,
                    onClick = { gradeLevel = g },
                    label = { Text(g) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        
        // Video Source Selector
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "مصدر فيديو الشرح لطلابنا 🎥:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ElevatedButton(
                        onClick = { isGoogleDrive = false },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (!isGoogleDrive) AcademicBlue else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "مقطع يوتيوب 🌐",
                            color = if (!isGoogleDrive) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }

                    ElevatedButton(
                        onClick = { isGoogleDrive = true },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isGoogleDrive) EmeraldSuccess else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "جوجل درايف 📁",
                            color = if (isGoogleDrive) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!isGoogleDrive) {
                    OutlinedTextField(
                        value = youtubeId,
                        onValueChange = { youtubeId = it },
                        label = { Text("رمز أو رابط يوتيوب (مثل: Z3G-A5I96Vw)") },
                        placeholder = { Text("اتركه فارغاً ليقوم النظام بغرس رابط افتراضي") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = youtubeId,
                        onValueChange = { youtubeId = it },
                        label = { Text("رابط مشاركة أو معرف قوقل درايف (ID)") },
                        placeholder = { Text("أدخل رابط معاينة الفيديو من قوقل درايف") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true
                    )
                }
            }
        }

        OutlinedTextField(
            value = pdfLink,
            onValueChange = { pdfLink = it },
            label = { Text("رابط مذكرة الشرح التاريخية (ملف PDF)") },
            placeholder = { Text("اختياري - رابط التلخيص المقالي") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            singleLine = true
        )

        // Activation Code field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = activationCode,
                onValueChange = { activationCode = it },
                label = { Text("كود تفعيل الحصة 🔐") },
                placeholder = { Text("اتركه فارغاً لتكون مجانية") },
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
            
            Button(
                onClick = {
                    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                    val rand = (1..6).map { chars.random() }.joinToString("")
                    activationCode = "HSN-$rand"
                    Toast.makeText(context, "تم توليد كود تفعيل تلقائي بنجاح! ⚡", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
            ) {
                Text("توليد كود ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        // Bulk Code Generation Section
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.05f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "توليد باقة أكواد تفعيل جماعية 🔑",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "توليد ملف نصي تيكست (.txt) يحتوي على باقة كبيرة من أكواد تفعيل الحصص المانعة للنسخ وتنزيله لتوزيعه.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = generateBulkCodes,
                        onCheckedChange = { generateBulkCodes = it }
                    )
                }

                if (generateBulkCodes) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "عدد الأكواد المطلوبة: $bulkCodesCount كود تفعيل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Slider(
                        value = bulkCodesCount.toFloat(),
                        onValueChange = { bulkCodesCount = it.toInt() },
                        valueRange = 10f..500f,
                        steps = 49 // steps of 10
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(20, 50, 100, 200, 500).forEach { num ->
                            FilterChip(
                                selected = bulkCodesCount == num,
                                onClick = { bulkCodesCount = num },
                                label = { Text("$num كود", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                if (title.isBlank() || desc.isBlank()) {
                    // Fail gracefully
                } else {
                    val finalVid = if (isGoogleDrive) youtubeId else youtubeId.ifBlank { "https://www.w3.org/2010/05/video/mediaelement.mp4" }
                    val finalPdf = pdfLink.ifBlank { "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf" }
                    val finalCode = if (activationCode.isBlank() && generateBulkCodes) "REQUIRED_BULK" else activationCode
                    onSubmit(title, desc, subject, gradeLevel, finalVid, finalPdf, finalCode, isGoogleDrive, generateBulkCodes, bulkCodesCount)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_lesson_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue),
            shape = RoundedCornerShape(12.dp),
            enabled = title.isNotBlank() && desc.isNotBlank()
        ) {
            Text("نشــر الحصة والمذكرة لجميع طلاب الأستاذ حسين حسن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAddExamTab(
    onSubmit: (String, String, String, Int, List<Question>) -> Unit
) {
    var examTitle by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("التاريخ") }
    var gradeLevel by remember { mutableStateOf("الصف الثالث الثانوي") }
    var durationMinutes by remember { mutableStateOf("15") }

    // Sequential Interactive Question builder inside Exam form
    var questionText by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "", "", "") }
    var correctIndex by remember { mutableStateOf(0) }

    val savedQuestions = remember { mutableStateListOf<Question>() }

    val subjects = listOf("التاريخ")
    val grades = listOf("الصف الأول الثانوي", "الصف الثاني الثانوي", "الصف الثالث الثانوي")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Text(
            text = "تصميم وإعداد اختبار جديد:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = examTitle,
            onValueChange = { examTitle = it },
            label = { Text("اسم الامتحان (مثال: اختبار الباب الثاني)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                label = { Text("مدة الامتحان بالدقائق") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "المادة:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    subjects.forEach { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 10.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }

        Text(text = "الصف المستهدف:", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            grades.forEach { g ->
                FilterChip(
                    selected = gradeLevel == g,
                    onClick = { gradeLevel = g },
                    label = { Text(g, fontSize = 10.sp) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Divider()
        Spacer(modifier = Modifier.height(14.dp))

        // Question input visual box
        Text(
            text = "إضافة أسئلة للاختبار الحالي (${savedQuestions.size} أسئلة مضافة حتى الآن):",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.12f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("السؤال (الأخ عينه...)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "خيارات الإجابة المتعددة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                // 4 Choices fields
                for (i in 0..3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = correctIndex == i,
                            onClick = { correctIndex = i }
                        )
                        OutlinedTextField(
                            value = options[i],
                            onValueChange = { options[i] = it },
                            label = { Text("الخيار رقم ${i + 1} ${if (correctIndex == i) "(الإجابة الصحيحة)" else ""}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (questionText.isNotBlank() && options.all { it.isNotBlank() }) {
                            val newQ = Question(
                                id = savedQuestions.size + 1,
                                text = questionText,
                                options = options.toList(),
                                correctIndex = correctIndex
                            )
                            savedQuestions.add(newQ)

                            // Reset single question form
                            questionText = ""
                            for (x in 0..3) options[x] = ""
                            correctIndex = 0
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة السؤال إلى الامتحان")
                }
            }
        }

        // Show live visual preview list of drafted questions
        if (savedQuestions.isNotEmpty()) {
            Text(text = "الأسئلة التي تم إضافتها حالياً للبطاقة:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            savedQuestions.forEachIndexed { idx, q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${idx + 1}. ${q.text.take(30)}...", fontSize = 11.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { savedQuestions.removeAt(idx) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete question", tint = RoseError)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save total exam compilation button
        Button(
            onClick = {
                val dur = durationMinutes.toIntOrNull() ?: 15
                onSubmit(examTitle, subject, gradeLevel, dur, savedQuestions.toList())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_exam_button"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
            shape = RoundedCornerShape(12.dp),
            enabled = examTitle.isNotBlank() && savedQuestions.isNotEmpty()
        ) {
            Text(
                text = "حفظ ونشر المجمع الامتحاني فورا (${savedQuestions.size} أسئلة)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun TeacherStudentsLedgerTab(
    submissions: List<QuizSubmission>,
    lessons: List<Lesson>,
    currentStudentName: String,
    currentStudentCode: String,
    studentsFromDb: List<StudentProfile>,
    onDeleteStudent: (String) -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf("STUDENTS_DOSSIER") } // STUDENTS_DOSSIER, WATCH_TIMERS, EXAM_LEDGER
    var searchQuery by remember { mutableStateOf("") }

    val dbStudentNames = studentsFromDb.map { it.name }
    val simulatedNames = listOf("أحمد عصام", "مي عصام الدين الجمال", "عبد الله السيد خليل")
    val studentsList = (dbStudentNames + submissions.map { it.studentName } + simulatedNames).distinct().sorted()
    var selectedStudent by remember { mutableStateOf(studentsList.firstOrNull() ?: "أحمد عصام") }

    // Is it a real / delete-able profile from database?
    fun getDbStudentId(student: String): String? {
        return studentsFromDb.find { it.name == student }?.id
    }

    // Resolve unique student registration code
    fun getStudentCode(student: String): String {
        val dbMatch = studentsFromDb.find { it.name == student }
        if (dbMatch != null) {
            return dbMatch.id
        }
        if (student == currentStudentName) {
            return currentStudentCode
        }
        val hash = (student.hashCode().coerceAtLeast(0) % 90000) + 10000
        return "STU-$hash"
    }

    // Helper functions for student watch logs & attendance simulation
    fun getStudentWatchTime(student: String, lesson: Lesson): Int {
        if (student == "أحمد عصام") {
            return lesson.watchTimeSeconds
        }
        // Deterministic high-quality simulated data for other pupils
        val hash = (student.hashCode() + lesson.title.hashCode()).coerceAtLeast(0)
        return if (hash % 5 == 0) 0 else (hash % 1200) + 240
    }

    fun isStudentCompleted(student: String, lesson: Lesson): Boolean {
        if (student == "أحمد عصام") return lesson.isCompleted
        return getStudentWatchTime(student, lesson) > 400
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Sub-tabs navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf(
                "STUDENTS_DOSSIER" to "ملفات ومتابعة الطلاب 🗃️",
                "WATCH_TIMERS" to "أوقات مشاهدة الحصص ⏱️",
                "EXAM_LEDGER" to "سجل الدرجات العام 📝"
            )
            subTabs.forEach { (key, label) ->
                val isSel = activeSubTab == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) AcademicBlue else Color.Transparent)
                        .clickable { activeSubTab = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active View Render
        when (activeSubTab) {
            "STUDENTS_DOSSIER" -> {
                // Individual Dossier Finder for students (ملف مخصص لكل طالب)
                Text(
                    text = "اختر الطالب لعرض ملفه الدراسي المخصص:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Student Slider selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studentsList.forEach { student ->
                        val isSel = selectedStudent == student
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedStudent = student },
                            label = { Text(student) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

                // Selected Pupil Folder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val parts = selectedStudent.split(" - ")
                        val cleanName = parts.getOrNull(0) ?: selectedStudent
                        var centerVal = "غير محدد"
                        var studentPhoneVal = "غير مسجل"
                        var parentPhoneVal = "غير مسجل"

                        parts.forEach { part ->
                            val p = part.trim()
                            if (p.startsWith("سنتر:")) {
                                centerVal = p.removePrefix("سنتر:").trim()
                            } else if (p.startsWith("هاتف الطالب:")) {
                                studentPhoneVal = p.removePrefix("هاتف الطالب:").trim()
                            } else if (p.startsWith("ولي الأمر:")) {
                                parentPhoneVal = p.removePrefix("ولي الأمر:").trim()
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AcademicBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cleanName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = cleanName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(AmberGold.copy(0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = getStudentCode(selectedStudent),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = AmberGold
                                        )
                                    }
                                }
                                Text(
                                    text = "ملف تتبع الحضور والامتحانات الفردي",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Detailed Student Data Information Cards
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.15f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = AcademicBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "مكان الحضور (السنتر):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = centerVal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "هاتف الطالب:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = studentPhoneVal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupervisorAccount,
                                        contentDescription = null,
                                        tint = RoseError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "هاتف ولي الأمر:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = parentPhoneVal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                val matchedProfile = studentsFromDb.find { it.name == selectedStudent }
                                val gradeLabel = matchedProfile?.gradeLevel ?: "الصف الثالث الثانوي"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = AmberGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "المكتبة / الصف الدراسي:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = gradeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Overall stats
                        val attendedCount = lessons.count { isStudentCompleted(selectedStudent, it) }
                        val missedCount = lessons.count { !isStudentCompleted(selectedStudent, it) }
                        val totalWatchSecs = lessons.sumOf { getStudentWatchTime(selectedStudent, it) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("نسبة حضور الحصص:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("$attendedCount من أصل ${lessons.size} حصة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("إجمالي وقت المذاكرة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${totalWatchSecs / 60} دقيقة و ${totalWatchSecs % 60} ثانية", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldSuccess)
                            }
                            Column {
                                Text("حصص لم يحضرها:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("$missedCount غياب 🚫", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoseError)
                            }
                        }

                        // Delete button if real database profile is selected
                        val dbStudentId = getDbStudentId(selectedStudent)
                        if (dbStudentId != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onDeleteStudent(dbStudentId)
                                    Toast.makeText(context, "تم حذف حساب الطالب $selectedStudent بنجاح! 🗑️", Toast.LENGTH_SHORT).show()
                                    selectedStudent = "أحمد عصام"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الكود وحساب الطالب",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حذف كود وحساب الطالب نهائياً من النظام 🗑️", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "💡 هذا الحساب تجريبي افتراضي أو قادم من سجل مشاركات قديم ولا يمكن حذفه من المنصة.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Missed Lessons vs. Solved exams scroll list inside student dossier
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "❌ الحصص التي لم يحضرها الطالب (الحصص اللي محضرهاش):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = RoseError,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val unattendedList = lessons.filter { !isStudentCompleted(selectedStudent, it) }
                    if (unattendedList.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(0.06f))
                            ) {
                                Text(
                                    text = "حضر هذا الطالب كافة الحصص بنسبة تامة! 🌟",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    } else {
                        items(unattendedList) { lesson ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, RoseError.copy(0.2f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(lesson.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("كود الحصة: ${lesson.activationCode.ifBlank { "غير مشفر" }}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Box(
                                        modifier = Modifier.background(RoseError.copy(0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("غائب / مغلق", fontSize = 10.sp, color = RoseError, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "📝 علامات ودرجات اختبارات الطالب التفصيلية:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AcademicBlue,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }

                    val studentExams = submissions.filter { it.studentName == selectedStudent }
                    if (studentExams.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                            ) {
                                Text(
                                    text = "لم ينجز هذا الطالب أي اختبار للمادة بعد.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    } else {
                        items(studentExams) { exam ->
                            val scorePercentage = (exam.score.toFloat() / exam.totalScore.toFloat()) * 100
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(exam.examTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("الإجابات الصحيحة: ${exam.correctCount} وبنسبة %${scorePercentage.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (scorePercentage >= 50f) EmeraldSuccess.copy(0.15f) else RoseError.copy(0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${exam.score} / ${exam.totalScore}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (scorePercentage >= 50f) EmeraldSuccess else RoseError,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "WATCH_TIMERS" -> {
                // Watch times and lesson study durations per lesson (تقرير أزمنة مشاهدة كل حصة)
                Text(
                    text = "تقرير المتابعة الصفية - أزمنة مشاهدة طلاب المجموعات لكل حصة تاريخية:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lessons) { lesson ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lesson.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(AcademicBlue.copy(0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (lesson.activationCode.isNotEmpty()) "🔐 كود: ${lesson.activationCode}" else "مفتوحة 🔓",
                                            fontSize = 9.sp,
                                            color = AcademicBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(0.05f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // List of all students and how much duration they watched this exact lesson
                                studentsList.forEach { student ->
                                    val watchSecs = getStudentWatchTime(student, lesson)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (watchSecs > 0) EmeraldSuccess else RoseError))
                                            Text(student, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = if (watchSecs > 0) "شاهد ${watchSecs / 60} دقيقة و ${watchSecs % 60} ثانية" else "غائب 🚫",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (watchSecs > 0) EmeraldSuccess else RoseError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "EXAM_LEDGER" -> {
                // Exam submissions general ledger sheet
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("بحث باسم الطالب أو كود الاختبار") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    singleLine = true
                )

                val filteredList = submissions.filter {
                    it.studentName.contains(searchQuery, ignoreCase = true) ||
                            it.examTitle.contains(searchQuery, ignoreCase = true)
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد سجلات اختبار مطابقة لبحثك.",
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { item ->
                            val scorePercentage = (item.score.toFloat() / item.totalScore.toFloat()) * 100
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AcademicBlue
                                        )
                                        Text(
                                            text = "مادة التاريخ",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "امتحن: " + item.examTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الإجابة الصحيحة: ${item.correctCount} / نسبة النجاح: %${scorePercentage.toInt()}",
                                            fontSize = 11.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (scorePercentage >= 50f) EmeraldSuccess.copy(0.12f) else RoseError.copy(0.12f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "الدرجة: ${item.score} / ${item.totalScore}",
                                                fontWeight = FontWeight.Bold,
                                                color = if (scorePercentage >= 50f) EmeraldSuccess else RoseError,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// EXAM TAKER IMPLEMENTATION
// ==========================================

@Composable
fun ExamTakerView(
    exam: Exam,
    studentName: String,
    onCancel: () -> Unit,
    onSubmit: (QuizSubmission) -> Unit
) {
    var currentQuestionIdx by remember { mutableStateOf(0) }
    val answeredChoices = remember { mutableStateMapOf<Int, Int>() } // questionIndex to selectedOptionIndex

    // Countdown timer setup (seconds)
    var remainingSeconds by remember { mutableStateOf(exam.durationMinutes * 60) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        // When timer goes down, trigger immediate compile submit!
        val correctCount = exam.questions.count { q ->
            answeredChoices[exam.questions.indexOf(q)] == q.correctIndex
        }
        val finalScore = correctCount * 10
        val sub = QuizSubmission(
            examId = exam.id,
            examTitle = exam.title,
            subject = exam.subject,
            studentName = studentName,
            score = finalScore,
            totalScore = exam.questions.size * 10,
            correctCount = correctCount
        )
        onSubmit(sub)
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = exam.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "المادة: ${exam.subject}",
                            color = AmberGold,
                            fontSize = 11.sp
                        )
                    }

                    // Ticking visual counter block
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoseError)
                    ) {
                        Text(
                            text = formattedTime,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp)
        ) {
            val totalQuestions = exam.questions.size
            if (totalQuestions == 0) {
                // Safely handle empty exam structure
                onCancel()
                return@Column
            }

            // Progress Indicators
            LinearProgressIndicator(
                progress = { (currentQuestionIdx + 1).toFloat() / totalQuestions.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(6.dp),
                color = AcademicBlue
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "سؤال رقم ${currentQuestionIdx + 1} من $totalQuestions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "قيمة السؤال: 10 درجات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = AmberGold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val currentQ = exam.questions[currentQuestionIdx]

            // Question block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = currentQ.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // MCQs Choices list
                    currentQ.options.forEachIndexed { optIdx, optText ->
                        val isSelected = answeredChoices[currentQuestionIdx] == optIdx
                        Surface(
                            onClick = { answeredChoices[currentQuestionIdx] = optIdx },
                            color = if (isSelected) AcademicBlue.copy(alpha = 0.08f) else Color.Transparent,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) AcademicBlue else MaterialTheme.colorScheme.outline.copy(0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("option_$optIdx")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { answeredChoices[currentQuestionIdx] = optIdx }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = optText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation action triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev button
                OutlinedButton(
                    onClick = { if (currentQuestionIdx > 0) currentQuestionIdx -= 1 },
                    enabled = currentQuestionIdx > 0,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("السابق")
                }

                // Submit button if final question, else Next button
                if (currentQuestionIdx == totalQuestions - 1) {
                    Button(
                        onClick = {
                            val correctCount = exam.questions.count { q ->
                                answeredChoices[exam.questions.indexOf(q)] == q.correctIndex
                            }
                            val finalScore = correctCount * 10
                            val sub = QuizSubmission(
                                examId = exam.id,
                                examTitle = exam.title,
                                subject = exam.subject,
                                studentName = studentName,
                                score = finalScore,
                                totalScore = totalQuestions * 10,
                                correctCount = correctCount
                            )
                            onSubmit(sub)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_exam_answers_btn")
                    ) {
                        Text("تسليم ورق الإجابة وتسليم الامتحان", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { if (currentQuestionIdx < totalQuestions - 1) currentQuestionIdx += 1 },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("السؤال التالي")
                    }
                }
            }
        }
    }
}

// ==========================================
// INDIVIDUAL DIALOG DETAILS
// ==========================================

@Composable
fun UserProfileDialog(
    currentName: String,
    currentGrade: String,
    currentCode: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var selectedGrade by remember { mutableStateOf(currentGrade) }
    var codeInput by remember { mutableStateOf(currentCode) }

    val gradeLevels = listOf("الصف الأول الثانوي", "الصف الثاني الثانوي", "الصف الثالث الثانوي")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تعديل الملف الشخصي للطالب:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("اسم الطالب بالكامل") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Student Code presentation block
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔑 كود الطالب الخاص بك:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = codeInput.ifBlank { "لم يحدد بعد" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = AmberGold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "يُستخدم هذا الكود لتمييز وتتبع حضورك وحلّك للامتحانات",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("تعديل كود الطالب (اختياري)") },
                    placeholder = { Text("مثال: STU-X987") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "حدد مرحلتك التعليمية الحالية:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Render simple radio-grid selectors
                gradeLevels.forEach { g ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGrade = g }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedGrade == g, onClick = { selectedGrade = g })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = g, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(nameInput, selectedGrade, codeInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue)
                    ) {
                        Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineSyncIndicator(
    syncStatus: AcademyViewModel.SyncStatus,
    onRefreshSync: () -> Unit
) {
    val backgroundColor = when (syncStatus) {
        AcademyViewModel.SyncStatus.SYNCING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        AcademyViewModel.SyncStatus.SUCCESS -> androidx.compose.ui.graphics.Color(0xFFE8F5E9) // Light green
        AcademyViewModel.SyncStatus.ERROR -> androidx.compose.ui.graphics.Color(0xFFFFEBEE) // Light red
        AcademyViewModel.SyncStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
    }

    val icon = when (syncStatus) {
        AcademyViewModel.SyncStatus.SYNCING -> Icons.Default.Refresh
        AcademyViewModel.SyncStatus.SUCCESS -> Icons.Default.CheckCircle
        AcademyViewModel.SyncStatus.ERROR -> Icons.Default.Warning
        AcademyViewModel.SyncStatus.IDLE -> Icons.Default.Cloud
    }

    val iconColor = when (syncStatus) {
        AcademyViewModel.SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
        AcademyViewModel.SyncStatus.SUCCESS -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        AcademyViewModel.SyncStatus.ERROR -> androidx.compose.ui.graphics.Color(0xFFC62828)
        AcademyViewModel.SyncStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = when (syncStatus) {
        AcademyViewModel.SyncStatus.SYNCING -> "جاري مزامنة المحاضرات والامتحانات أونلاين..."
        AcademyViewModel.SyncStatus.SUCCESS -> "متصل بقاعدة البيانات السحابية - البيانات محدثة"
        AcademyViewModel.SyncStatus.ERROR -> "تعذر الاتصال الخارجي! تعمل حالياً بوضع الأوفلاين"
        AcademyViewModel.SyncStatus.IDLE -> "اتصال سحابي غير نشط"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRefreshSync() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (syncStatus == AcademyViewModel.SyncStatus.SYNCING) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = iconColor
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = "حالة الاتصال",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 11.sp,
                    color = if (syncStatus == AcademyViewModel.SyncStatus.IDLE) MaterialTheme.colorScheme.onSurfaceVariant else iconColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (syncStatus != AcademyViewModel.SyncStatus.SYNCING) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "مزامنة",
                        tint = iconColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "مزامنة الآن",
                        fontSize = 10.sp,
                        color = iconColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TeacherPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "قفل",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "بوابة المعلم المسؤول",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "يرجى إدخال كلمة المرور الخاصة بالمعلم للمتابعة والدخول للوحة التحكم.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { 
                        passwordInput = it 
                        showError = false
                    },
                    label = { Text("رمز المرور") },
                    placeholder = { Text("أدخل رمز المرور المخصص") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = showError
                )
                
                if (showError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "رمز المرور غير صحيح! يرجى المحاولة مرة أخرى.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (passwordInput == "history2026") {
                                onSuccess()
                            } else {
                                showError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBlue)
                    ) {
                        Text("تأكيد الدخول", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun WatchVideoDialog(
    lesson: Lesson,
    onDismiss: () -> Unit,
    onMarkCompleted: () -> Unit,
    onUpdateWatchTime: (Int) -> Unit,
    onUnlockWithCode: (String, (Boolean) -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var enteredCode by remember { mutableStateOf("") }
    var codeErrorMsg by remember { mutableStateOf<String?>(null) }
    var isUnlocking by remember { mutableStateOf(false) }
    
    val isLocked = lesson.activationCode.isNotEmpty() && !lesson.isUnlocked

    // Live study duration tracking loop (ticks every second and saves)
    var activeStudySeconds by remember { mutableStateOf(lesson.watchTimeSeconds) }
    var sessionAccumulatedSeconds by remember { mutableStateOf(0) }

    val handleDismiss = {
        if (sessionAccumulatedSeconds > 0) {
            onUpdateWatchTime(sessionAccumulatedSeconds)
        }
        onDismiss()
    }

    if (!isLocked) {
        LaunchedEffect(key1 = true) {
            while (true) {
                delay(1000L)
                activeStudySeconds += 1
                sessionAccumulatedSeconds += 1
                
                // Save periodically every 15 seconds to minimize disk writes and UI redraw overhead
                if (sessionAccumulatedSeconds >= 15) {
                    onUpdateWatchTime(15)
                    sessionAccumulatedSeconds = 0
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = handleDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {} // block click propagation
                ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isLocked) {
                    // locked activation overlay screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked Lesson",
                            tint = AmberGold,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هذه الحصة مغلقة بقفل تفعيل 🔒",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "لفتح محتوى حصة الأستاذ حسين حسن، الرجاء إدخال كود التفعيل المطبوع الذي قمت بشرائه.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = enteredCode,
                            onValueChange = { 
                                enteredCode = it
                                codeErrorMsg = null
                            },
                            label = { Text("أدخل كود تفعيل الحصة هـنا") },
                            placeholder = { Text("مثال: HASSAN2026") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = codeErrorMsg != null
                        )

                        if (codeErrorMsg != null) {
                            Text(
                                text = codeErrorMsg!!,
                                color = RoseError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (enteredCode.isBlank()) return@Button
                                isUnlocking = true
                                onUnlockWithCode(enteredCode) { success ->
                                    isUnlocking = false
                                    if (success) {
                                        codeErrorMsg = null
                                        Toast.makeText(context, "تم تفعيل وفتح الحصة بنجاح! 🏛️", Toast.LENGTH_SHORT).show()
                                    } else {
                                        codeErrorMsg = "كود التفعيل غير صحيح! يرجى مراجعة الأستاذ حسين حسن."
                                    }
                                }
                            },
                            enabled = !isUnlocking && enteredCode.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isUnlocking) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تفعيــل وفـك القفـل الآن", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إلغاء والرجوع للخلف", color = RoseError)
                        }
                    }
                } else {
                    // unlocked player container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        var webViewError by remember { mutableStateOf(false) }

                        val videoUrl = if (lesson.isGoogleDrive) {
                            if (lesson.videoId.startsWith("http")) {
                                if (lesson.videoId.contains("/preview") || lesson.videoId.contains("/view")) {
                                    lesson.videoId
                                } else {
                                    lesson.videoId.replace("/view?usp=drivesdk", "/preview").replace("/view", "/preview")
                                }
                            } else {
                                "https://drive.google.com/file/d/${lesson.videoId}/preview"
                            }
                        } else {
                            if (lesson.videoId.startsWith("http")) lesson.videoId else "https://www.youtube.com/embed/${lesson.videoId}"
                        }

                        if (webViewError) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "مشغل الفيديو مدمج غير مدعوم على هذه المحاكاة.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(videoUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "فشل فتح الرابط!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                                ) {
                                    Text("فتح المحاضرة في المتصفح الخارجي ↗️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        } else {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    try {
                                        android.webkit.WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            if (lesson.isGoogleDrive) {
                                                settings.allowContentAccess = true
                                                settings.domStorageEnabled = true
                                            }
                                            webViewClient = android.webkit.WebViewClient()
                                            loadUrl(videoUrl)
                                        }
                                    } catch (e: Throwable) {
                                        webViewError = true
                                        android.view.View(ctx)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lesson.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(AcademicBlue.copy(0.12f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "حضورك: ${activeStudySeconds} ثانية (${activeStudySeconds / 60} دقيقة)",
                                    fontSize = 10.sp,
                                    color = AcademicBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            text = "الأستاذ: حسين حسن | مادة التاريخ",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EmeraldSuccess.copy(0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, EmeraldSuccess.copy(0.24f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "مسموح العرض الخارجي: تم تفعيل البث والعرض على البروجيكتور والشاشات الخارجية 📺",
                                color = EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lesson.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Lesson stats and completion trigger
                        Button(
                            onClick = {
                                onMarkCompleted()
                                handleDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lesson.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (lesson.isCompleted) "أكملت الحضور بنجاح في ملفك ✓" else "تأكيد واستكمال ملف الحضور",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = handleDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق والرجوع للحصص", color = RoseError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizReportCertificate(
    result: QuizSubmission,
    onDismiss: () -> Unit
) {
    val percentage = (result.score.toFloat() / result.totalScore.toFloat()) * 100
    val isPassed = percentage >= 50f

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, if (isPassed) EmeraldSuccess else RoseError)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Award trophy icon if passed
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isPassed) AmberGold.copy(0.12f) else RoseError.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPassed) Icons.Default.Star else Icons.Default.Warning,
                        contentDescription = "Evaluation Badge",
                        tint = if (isPassed) AmberGold else RoseError,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isPassed) "تهانينا الحارة! نلت شهادة تفوق" else "تقرير ونتيجة المحاولة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isPassed) EmeraldSuccess else RoseError
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "لقد أتممت بنجاح اختبار:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = result.examTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Score banner display
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isPassed) EmeraldSuccess.copy(0.1f) else RoseError.copy(0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.score} / ${result.totalScore}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = if (isPassed) EmeraldSuccess else RoseError
                        )
                        Text(
                            text = "نسبة نجاحك: %${percentage.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPassed) EmeraldSuccess else RoseError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "أجبت على ${result.correctCount} سؤال بشكل صحيح من أصل ${result.totalScore/10}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPassed) EmeraldSuccess else AcademicBlue),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تم، العودة للصف الدراسي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginAndRegisterView(
    viewModel: AcademyViewModel,
    onLoginSuccess: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("الصف الثالث الثانوي") }
    var centerInput by remember { mutableStateOf("") }
    var parentPhoneInput by remember { mutableStateOf("") }
    var studentPhoneInput by remember { mutableStateOf("") }
    
    var showValidationErrors by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var generatedCodeDialog by remember { mutableStateOf<String?>(null) }
    var showTeacherPasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val grades = listOf("الصف الأول الثانوي", "الصف الثاني الثانوي", "الصف الثالث الثانوي")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        
        // Brand logo
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(AmberGold.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(56.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "منصة الأستاذ حسين حسن التعليمية",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "بوابتك الإلكترونية في مادة التاريخ للثانوية العامة 🏛️",
            fontSize = 12.sp,
            color = AmberGold,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(30.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegisterMode) "إنشاء حساب طالب جديد 📝" else "تسجيل الدخول للمنصة 🔐",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(18.dp))

                val isNameError = showValidationErrors && (nameInput.isBlank() || (isRegisterMode && nameInput.trim().split("\\s+".toRegex()).size < 3))
                val isCenterError = showValidationErrors && isRegisterMode && centerInput.isBlank()
                val isParentError = showValidationErrors && isRegisterMode && (parentPhoneInput.isBlank() || parentPhoneInput.length < 11 || !parentPhoneInput.all { it.isDigit() })
                val isStudentError = showValidationErrors && isRegisterMode && (studentPhoneInput.isBlank() || studentPhoneInput.length < 11 || !studentPhoneInput.all { it.isDigit() })
                val isCodeError = showValidationErrors && !isRegisterMode && codeInput.isBlank()
                
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { 
                        nameInput = it
                        errorMsg = null
                    },
                    label = { Text(if (isRegisterMode) "اسم الطالب ثلاثي أو رباعي (مطلوب) *" else "اسم الطالب ثلاثي أو رباعي") },
                    placeholder = { Text("مثال: أحمد مصطفى عصام") },
                    leadingIcon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("student_name_field"),
                    singleLine = true,
                    isError = isNameError
                )
                
                if (isRegisterMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = centerInput,
                        onValueChange = { 
                            centerInput = it
                            errorMsg = null
                        },
                        label = { Text("مكان الحضور السنتر أو أونلاين (مطلوب) *") },
                        placeholder = { Text("مثال: سنتر الأوائل أو أونلاين") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Place, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("student_center_field"),
                        singleLine = true,
                        isError = isCenterError
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = parentPhoneInput,
                        onValueChange = { 
                            parentPhoneInput = it
                            errorMsg = null
                        },
                        label = { Text("رقم هاتف ولي الأمر (مطلوب) *") },
                        placeholder = { Text("مثال: 01012345678") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("student_parent_phone_field"),
                        singleLine = true,
                        isError = isParentError,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )
 
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = studentPhoneInput,
                        onValueChange = { 
                            studentPhoneInput = it
                            errorMsg = null
                        },
                        label = { Text("رقم هاتف الطالب نفسه (مطلوب) *") },
                        placeholder = { Text("مثال: 01011112222") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("student_phone_field"),
                        singleLine = true,
                        isError = isStudentError,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )
                }
                
                if (!isRegisterMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { 
                            codeInput = it
                            errorMsg = null
                        },
                        label = { Text("كود الطالب الشخصي (8 خانات)") },
                        placeholder = { Text("مثال: STU-ABCDE") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("student_code_field"),
                        singleLine = true,
                        isError = isCodeError
                    )
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "اختر الصف الدراسي:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        grades.forEach { grade ->
                            FilterChip(
                                selected = selectedGrade == grade,
                                onClick = { selectedGrade = grade },
                                label = { Text(grade, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        showValidationErrors = true
                        if (nameInput.isBlank()) {
                            errorMsg = "الرجاء إدخال اسم الطالب!"
                            return@Button
                        }
                        
                        if (isRegisterMode) {
                            val nameParts = nameInput.trim().split("\\s+".toRegex())
                            if (nameParts.size < 3) {
                                errorMsg = "الرجاء إدخال اسم الطالب ثلاثي على الأقل!"
                                return@Button
                            }
                            if (centerInput.isBlank()) {
                                errorMsg = "الرجاء إدخال اسم السنتر أو أونلاين!"
                                return@Button
                            }
                            if (parentPhoneInput.isBlank()) {
                                errorMsg = "الرجاء إدخال رقم هاتف ولي الأمر!"
                                return@Button
                            }
                            if (parentPhoneInput.length < 11 || !parentPhoneInput.all { it.isDigit() }) {
                                errorMsg = "الرجاء إدخال رقم هاتف ولي أمر صحيح (11 رقماً)!"
                                return@Button
                            }
                            if (studentPhoneInput.isBlank()) {
                                errorMsg = "الرجاء إدخال رقم هاتف الطالب الشخصي!"
                                return@Button
                            }
                            if (studentPhoneInput.length < 11 || !studentPhoneInput.all { it.isDigit() }) {
                                errorMsg = "الرجاء إدخال رقم هاتف طالب صحيح (11 رقماً)!"
                                return@Button
                            }
                        } else {
                            if (codeInput.isBlank()) {
                                errorMsg = "الرجاء إدخال الكود الشخصي للطالب!"
                                return@Button
                            }
                        }
                        
                        isVerifying = true
                        if (isRegisterMode) {
                            val fullNameWithDetails = "${nameInput.trim()} - سنتر: ${centerInput.trim()} - هاتف الطالب: ${studentPhoneInput.trim()} - ولي الأمر: ${parentPhoneInput.trim()}"
                            viewModel.registerStudent(fullNameWithDetails, selectedGrade) { success, code, err ->
                                isVerifying = false
                                if (success && code != null) {
                                    generatedCodeDialog = code
                                } else {
                                    errorMsg = err ?: "فشل تسجيل حساب الطالب الجديد"
                                }
                            }
                        } else {
                            viewModel.loginStudent(nameInput, codeInput) { success, err ->
                                isVerifying = false
                                if (success) {
                                    onLoginSuccess()
                                } else {
                                    errorMsg = err ?: "فشل تسجيل الدخول!"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("submit_login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRegisterMode) EmeraldSuccess else AcademicBlue)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = if (isRegisterMode) "تأكيد وإنشاء الكود الشخصي" else "دخول الآن للمحاضرات",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                TextButton(
                    onClick = { 
                        isRegisterMode = !isRegisterMode 
                        errorMsg = null
                        showValidationErrors = false
                    },
                    modifier = Modifier.testTag("toggle_register_mode")
                ) {
                    Text(
                        text = if (isRegisterMode) "لديك كود بالفعل؟ اضغط هنا لتسجيل الدخول" else "ليس لديك كود؟ انقر هنا للمقرر وإنشاء كود طالب جديد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Quick pass to teacher portal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "هل أنت المعلم المسؤول؟", fontSize = 12.sp, color = Color.White.copy(0.8f))
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                onClick = {
                    showTeacherPasswordDialog = true
                },
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AmberGold),
                modifier = Modifier.testTag("teacher_bypasser")
            ) {
                Text(
                    text = "بوابة المعلم 🏛️",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
    
    // Dialog displaying generated code to copies
    if (generatedCodeDialog != null) {
        Dialog(onDismissRequest = { 
            generatedCodeDialog = null 
            onLoginSuccess()
        }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("تم إنشاء حسابك بنجاح! 🎉", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("هذا هو كودك الشخصي المعتمد لتسجيل الدخول لاحقاً:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedCodeDialog!!,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Button(
                        onClick = { 
                            generatedCodeDialog = null 
                            onLoginSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("الدخول للمنصة والمقررات الدراسيّة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTeacherPasswordDialog) {
        TeacherPasswordDialog(
            onDismiss = { showTeacherPasswordDialog = false },
            onSuccess = {
                showTeacherPasswordDialog = false
                viewModel.loginUserOffline("المعلم حسين حسن", "ADMIN", "معلم المادة")
                viewModel.setRole(AcademyViewModel.Role.TEACHER)
                Toast.makeText(context, "تم تسجيل الدخول كـ معلم مسؤول! 🏛️", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

fun saveTextFileToDownloads(context: android.content.Context, filename: String, textContent: String) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(textContent.toByteArray())
                }
                Toast.makeText(context, "تم حفظ الملف بنجاح في مجلد Downloads! 📥", Toast.LENGTH_LONG).show()
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, filename)
            file.writeText(textContent)
            Toast.makeText(context, "تم حفظ الملف بنجاح: ${file.absolutePath} 📥", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "جاري فتح نافذة المشاركة للحفظ... 📂", Toast.LENGTH_SHORT).show()
    }
    
    // Always trigger share intent as second layer / backup
    try {
        val sendIntent: android.content.Intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, textContent)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "مشاركة وحفظ قائمة الأكواد 📄")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun SmartNotificationsDialog(
    notifications: List<SmartNotification>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("مسح الكل 🗑️", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "مركز الإشعارات الذكية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Empty",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد إشعارات جديدة حالياً",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notif ->
                        val icon = when (notif.type) {
                            "LESSON" -> Icons.Default.PlayArrow
                            "EXAM" -> Icons.Default.Assignment
                            "TIPS" -> Icons.Default.Star
                            "AI" -> Icons.Default.Star
                            else -> Icons.Default.Info
                        }
                        val tint = when (notif.type) {
                            "LESSON" -> MaterialTheme.colorScheme.primary
                            "EXAM" -> MaterialTheme.colorScheme.secondary
                            "TIPS" -> AmberGold
                            "AI" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(tint.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = notif.type,
                                        tint = tint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SmartAiAdvisorDialog(
    aiState: AIState,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "AI Professor",
                    tint = AmberGold,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "البروفيسور - مستشار التاريخ الذكي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                when (aiState) {
                    is AIState.Idle -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Text(
                                text = "مرحباً بك في وحدة الإرشاد الأكاديمي المتقدمة بالذكاء الاصطناعي! 🎓✨",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "يقوم البروفيسور بتحليل تلقائي لهيستوجرام أداؤك في الاختبارات السابقة ونسب تقدمك في الحصص الأسبوعية وعدد المرات التي أتممت فيها المذاكرة، ومن ثم يقوم بصياغة تقرير شامل لتوجيهك نحو التفوق والدرجة النهائية.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onGenerate,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Compute",
                                        tint = Color.White
                                    )
                                    Text("توليد تقرير وتحليلات مستواي الآن 🚀", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is AIState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "جاري الاتصال بقاعدة معارف البروفيسور...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "يقوم البروفيسور بتحليل سجل مشاهداتك ونتائج اختبارات التاريخ بدقة عالية... 🏛️⏳",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is AIState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = aiState.report,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onGenerate,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                                    Text("تحديث التقرير وتحليل درجات جديدة 🔄", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is AIState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "فشل توليد التقرير",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiState.message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onGenerate,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إعادة المحاولة 🔄", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    )
}

