package com.example.campuscomplaint

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DbHelper(context: Context) : SQLiteOpenHelper(context, "campus.db", null, 2), Repository<Complaint> {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE users(
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL,
            email TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE complaints(
            id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL,
            category TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
            location TEXT NOT NULL, photo_path TEXT, status TEXT NOT NULL DEFAULT 'Pending',
            created_at INTEGER NOT NULL, urgency INTEGER NOT NULL DEFAULT 3,
            incident_time INTEGER NOT NULL DEFAULT 0, rating INTEGER NOT NULL DEFAULT 0,fixed_photo_path TEXT)""")
        val admin = ContentValues().apply {
            put("name", "Admin"); put("email", "admin@campus.com")
            put("password", hash("admin123")); put("role", Role.ADMIN.value)
        }
        db.insert("users", null, admin)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {if (oldV < 2) db.execSQL("ALTER TABLE complaints ADD COLUMN fixed_photo_path TEXT")
    }

    private fun hash(s: String) = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    // ---- users ----
    fun registerStudent(name: String, email: String, password: String): Boolean {
        val cv = ContentValues().apply {
            put("name", name); put("email", email.lowercase())
            put("password", hash(password)); put("role", Role.STUDENT.value)
        }
        return try { writableDatabase.insertOrThrow("users", null, cv); true }
        catch (e: SQLiteConstraintException) { false }
    }

    fun login(email: String, password: String, role: String): User? {
        readableDatabase.rawQuery(
            "SELECT id, name, email, role FROM users WHERE email=? AND password=? AND role=?",
            arrayOf(email.lowercase(), hash(password), role)
        ).use { c ->
            return if (c.moveToFirst()) User(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)) else null
        }
    }

    // ---- CREATE ----
    fun addComplaint(studentId: Long, category: String, title: String, description: String,
                     location: String, photoPath: String?, urgency: Int, incidentTime: Long) {
        val cv = ContentValues().apply {
            put("student_id", studentId); put("category", category); put("title", title)
            put("description", description); put("location", location); put("photo_path", photoPath)
            put("status", Status.PENDING); put("created_at", System.currentTimeMillis())
            put("urgency", urgency); put("incident_time", incidentTime)
        }
        writableDatabase.insert("complaints", null, cv)
    }

    // ---- READ ----
    fun getComplaints(studentId: Long?): List<Complaint> =
        if (studentId == null) query("", null) else query("WHERE c.student_id=?", arrayOf(studentId.toString()))

    override fun getAll() = getComplaints(null)
    override fun getById(id: Long): Complaint? = query("WHERE c.id=?", arrayOf(id.toString())).firstOrNull()

    // UPDATE: returns false if refused (Resolved needs a fixed photo)
    fun updateStatus(id: Long, status: String): Boolean {
        if (status == Status.RESOLVED) {
            val hasPhoto = readableDatabase.rawQuery(
                "SELECT fixed_photo_path FROM complaints WHERE id=?", arrayOf(id.toString())
            ).use { c -> c.moveToFirst() && !c.getString(0).isNullOrEmpty() }
            if (!hasPhoto) return false
        }
        val cv = ContentValues().apply { put("status", status) }
        writableDatabase.update("complaints", cv, "id=?", arrayOf(id.toString()))
        return true
    }
    fun saveRating(id: Long, rating: Int) {
        val cv = ContentValues().apply { put("rating", rating) }
        writableDatabase.update("complaints", cv, "id=?", arrayOf(id.toString()))
    }

    fun saveFixedPhoto(id: Long, path: String) {
        val cv = ContentValues().apply { put("fixed_photo_path", path) }
        writableDatabase.update("complaints", cv, "id=?", arrayOf(id.toString()))
    }
    // ---- DELETE ----
    override fun delete(id: Long) {
        writableDatabase.delete("complaints", "id=?", arrayOf(id.toString()))
    }

    private fun query(where: String, args: Array<String>?): List<Complaint> {
        val sql = "SELECT c.*, u.name AS student_name FROM complaints c " +
                "JOIN users u ON u.id = c.student_id $where ORDER BY c.created_at DESC"
        val list = mutableListOf<Complaint>()
        readableDatabase.rawQuery(sql, args).use { c -> while (c.moveToNext()) list += c.toComplaint() }
        return list
    }

    private fun Cursor.toComplaint(): Complaint {
        fun s(n: String): String? = getString(getColumnIndexOrThrow(n))
        fun l(n: String) = getLong(getColumnIndexOrThrow(n))
        fun i(n: String) = getInt(getColumnIndexOrThrow(n))
        return Complaint(l("id"), l("student_id"), s("student_name")!!, s("category")!!, s("title")!!,
            s("description")!!, s("location")!!, s("photo_path"), s("status")!!, l("created_at"),
            i("urgency"), l("incident_time"), i("rating"), s("fixed_photo_path"))
    }
}