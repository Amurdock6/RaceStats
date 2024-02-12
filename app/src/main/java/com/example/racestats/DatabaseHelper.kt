package com.example.racestats

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "service_records.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE IF NOT EXISTS service_records (id INTEGER PRIMARY KEY AUTOINCREMENT, service TEXT, mileage TEXT, date TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Implement if you need to upgrade the database schema in the future
    }

    // Function to update a record in the database
    fun updateRecord(recordId: Long, newService: String, newMileage: String, newDate: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("service", newService)
            put("mileage", newMileage)
            put("date", newDate)
        }

        // The update method returns the number of rows affected by the update
        val rowsAffected = db.update(
            "service_records",
            values,
            "id = ?",
            arrayOf(recordId.toString())
        )

        // Close the database after the operation
        db.close()

        return rowsAffected
    }
}
