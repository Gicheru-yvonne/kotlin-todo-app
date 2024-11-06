package com.example.todofinal

import android.content.ContentValues
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AddTodoListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AddTodoListScreen { listName ->
                if (listName.isNotBlank()) {
                    val dbHelper = TodoDatabaseHelper(this)
                    val db = dbHelper.readableDatabase

                    if (isListNameDuplicate(dbHelper, listName)) {
                        Toast.makeText(this, "A Todo List with this name already exists", Toast.LENGTH_SHORT).show()
                    } else {

                        val values = ContentValues().apply {
                            put(TodoDatabaseHelper.COLUMN_NAME, listName)
                        }

                        val result = db.insert(TodoDatabaseHelper.TABLE_TODO_LIST, null, values)
                        db.close()

                        if (result != -1L) {
                            Toast.makeText(this, "Todo List Added", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to Add Todo List", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Todo List Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun isListNameDuplicate(dbHelper: TodoDatabaseHelper, listName: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${TodoDatabaseHelper.TABLE_TODO_LIST} WHERE ${TodoDatabaseHelper.COLUMN_NAME} = ?",
            arrayOf(listName)
        )
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        db.close()
        return count > 0
    }
}

@Composable
fun AddTodoListScreen(onSaveClick: (String) -> Unit) {
    var listName by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = listName,
            onValueChange = { listName = it },
            label = { Text("Todo List Name") }
        )

        Button(
            onClick = { onSaveClick(listName) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save Todo List")
        }
    }
}
