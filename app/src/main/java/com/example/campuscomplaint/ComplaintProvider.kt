package com.example.campuscomplaint

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

class ComplaintProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.example.campuscomplaint.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/complaints")
    }
    private lateinit var helper: DbHelper

    override fun onCreate(): Boolean { helper = DbHelper(context!!); return true }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? =
        helper.readableDatabase.query("complaints", projection, selection, selectionArgs, null, null, sortOrder)

    override fun getType(uri: Uri) = "vnd.android.cursor.dir/vnd.$AUTHORITY.complaints"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0
}