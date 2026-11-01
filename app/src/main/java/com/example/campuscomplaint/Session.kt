package com.example.campuscomplaint


import android.content.Context

class Session(ctx: Context) {
    private val p = ctx.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun save(u: User) = p.edit().putLong("id", u.id).putString("name", u.name)
        .putString("email", u.email).putString("role", u.role).apply()

    fun user(): User? {
        val id = p.getLong("id", -1)
        if (id == -1L) return null
        return User(id, p.getString("name", "")!!, p.getString("email", "")!!,
            p.getString("role", Role.STUDENT.value)!!)
    }

    fun clear() = p.edit().clear().apply()
}