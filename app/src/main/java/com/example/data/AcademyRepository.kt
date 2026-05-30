package com.example.data

import kotlinx.coroutines.flow.Flow

class AcademyRepository(private val academyDao: AcademyDao) {

    // Lessons
    val allLessons: Flow<List<Lesson>> = academyDao.getAllLessons()

    fun getLessonsByGrade(gradeLevel: String): Flow<List<Lesson>> {
        return academyDao.getLessonsByGrade(gradeLevel)
    }

    suspend fun insertLesson(lesson: Lesson) {
        academyDao.insertLesson(lesson)
    }

    suspend fun updateLesson(lesson: Lesson) {
        academyDao.updateLesson(lesson)
    }

    suspend fun deleteLesson(lessonId: Int) {
        academyDao.deleteLesson(lessonId)
    }

    // Exams
    val allExams: Flow<List<Exam>> = academyDao.getAllExams()

    fun getExamsByGrade(gradeLevel: String): Flow<List<Exam>> {
        return academyDao.getExamsByGrade(gradeLevel)
    }

    suspend fun getExamById(examId: Int): Exam? {
        return academyDao.getExamById(examId)
    }

    suspend fun insertExam(exam: Exam) {
        academyDao.insertExam(exam)
    }

    suspend fun deleteExam(examId: Int) {
        academyDao.deleteExam(examId)
    }

    // Submissions
    val allSubmissions: Flow<List<QuizSubmission>> = academyDao.getAllSubmissions()

    fun getSubmissionsByStudent(studentName: String): Flow<List<QuizSubmission>> {
        return academyDao.getSubmissionsByStudent(studentName)
    }

    suspend fun insertSubmission(submission: QuizSubmission) {
        academyDao.insertSubmission(submission)
    }

    // Students / Profiles
    val allStudents: Flow<List<StudentProfile>> = academyDao.getAllStudents()

    suspend fun insertStudent(profile: StudentProfile) {
        academyDao.insertStudent(profile)
    }

    suspend fun deleteStudent(studentId: String) {
        academyDao.deleteStudent(studentId)
    }

    suspend fun getStudentsOnce(): List<StudentProfile> {
        return academyDao.getStudentsOnce()
    }

    // Activation Codes
    val allActivationCodes: Flow<List<ActivationCode>> = academyDao.getAllActivationCodes()

    suspend fun insertActivationCode(code: ActivationCode) {
        academyDao.insertActivationCode(code)
    }

    suspend fun getActivationCodesOnce(): List<ActivationCode> {
        return academyDao.getActivationCodesOnce()
    }
}
