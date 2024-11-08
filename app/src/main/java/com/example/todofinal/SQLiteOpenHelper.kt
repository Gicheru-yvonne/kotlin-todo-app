package com.example.todofinal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class TodoDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "todolist.db"
        private const val DATABASE_VERSION = 5
        const val TABLE_TODO_LIST = "todolist"
        const val TABLE_TODO_ITEM = "todoitem"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_LIST_ID = "list_id"
        const val COLUMN_ITEM_NAME = "item_name"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_COMPLETED = "completed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTodoListTable = "CREATE TABLE $TABLE_TODO_LIST (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT NOT NULL);"
        val createTodoItemTable = "CREATE TABLE $TABLE_TODO_ITEM (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_ITEM_NAME TEXT NOT NULL, " +
                "$COLUMN_DUE_DATE TEXT, " +
                "$COLUMN_LIST_ID INTEGER, " +
                "$COLUMN_COMPLETED INTEGER DEFAULT 0, " +
                "FOREIGN KEY($COLUMN_LIST_ID) REFERENCES $TABLE_TODO_LIST($COLUMN_ID) ON DELETE CASCADE);"

        db.execSQL(createTodoListTable)
        db.execSQL(createTodoItemTable)
        Log.d("TodoDatabaseHelper", "Database tables created.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TODO_ITEM")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TODO_LIST")
        onCreate(db)
    }


    fun isListNameDuplicate(listName: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TODO_LIST WHERE $COLUMN_NAME = ?",
            arrayOf(listName)
        )
        var isDuplicate = false
        if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            isDuplicate = count > 0
        }
        cursor.close()
        db.close()
        return isDuplicate
    }


    fun getItemCountsForList(listId: Int): Pair<Int, Int> {
        val db = readableDatabase
        var totalItems = 0
        var completedItems = 0

        val totalCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TODO_ITEM WHERE $COLUMN_LIST_ID = ?",
            arrayOf(listId.toString())
        )
        if (totalCursor.moveToFirst()) {
            totalItems = totalCursor.getInt(0)
        }
        totalCursor.close()

        val completedCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TODO_ITEM WHERE $COLUMN_LIST_ID = ? AND $COLUMN_COMPLETED = 1",
            arrayOf(listId.toString())
        )
        if (completedCursor.moveToFirst()) {
            completedItems = completedCursor.getInt(0)
        }
        completedCursor.close()
        db.close()

        return Pair(totalItems, completedItems)
    }


    fun updateTodoItem(itemId: Int, newItemName: String, newDueDate: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ITEM_NAME, newItemName)
            put(COLUMN_DUE_DATE, newDueDate)
        }

        return try {
            val rowsAffected = db.update(
                TABLE_TODO_ITEM,
                values,
                "$COLUMN_ID = ?",
                arrayOf(itemId.toString())
            )
            if (rowsAffected > 0) {
                Log.d("TodoDatabaseHelper", "Successfully updated item: $newItemName with ID: $itemId")
                true
            } else {
                Log.w("TodoDatabaseHelper", "No rows updated for item with ID: $itemId")
                false
            }
        } catch (e: Exception) {
            Log.e("TodoDatabaseHelper", "Error updating item with ID: $itemId", e)
            false
        } finally {
            db.close()
        }
    }


    fun moveTodoItem(itemId: Int, newListId: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LIST_ID, newListId)
        }

        return try {
            val rowsAffected = db.update(
                TABLE_TODO_ITEM,
                values,
                "$COLUMN_ID = ?",
                arrayOf(itemId.toString())
            )
            if (rowsAffected > 0) {
                Log.d("TodoDatabaseHelper", "Successfully moved item with ID: $itemId to new list ID: $newListId")
                true
            } else {
                Log.w("TodoDatabaseHelper", "No rows updated while moving item with ID: $itemId")
                false
            }
        } catch (e: Exception) {
            Log.e("TodoDatabaseHelper", "Error moving item with ID: $itemId", e)
            false
        } finally {
            db.close()
        }
    }
}
