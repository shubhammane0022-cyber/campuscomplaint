package com.example.campuscomplaint

enum class Role(val value: String) { STUDENT("student"), ADMIN("admin") }

object Status {
    const val PENDING = "Pending"
    const val IN_PROGRESS = "In Progress"
    const val RESOLVED = "Resolved"
    const val REJECTED = "Rejected"
}

data class User(val id: Long, val name: String, val email: String, val role: String)

data class Complaint(
    val id: Long, val studentId: Long, val studentName: String,
    val category: String, val title: String, val description: String,
    val location: String, val photoPath: String?, val status: String, val createdAt: Long,
    val urgency: Int, val incidentTime: Long, val rating: Int,
    val fixedPhotoPath: String?
)

sealed class AuthResult {                       // sealed class
    data class Success(val user: User) : AuthResult()
    object WrongCredentials : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface Repository<T> {                       // generic interface
    fun getAll(): List<T>
    fun getById(id: Long): T?
    fun delete(id: Long)
}

interface ComplaintActions {                    // interface used as a callback
    fun onView(c: Complaint)
    fun onReject(c: Complaint)
    fun onDelete(c: Complaint)
    fun onShare(c: Complaint)
}