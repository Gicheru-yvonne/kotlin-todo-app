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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class EditTodoListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val listId = intent.getIntExtra("list_id", -1)
        val currentName = intent.getStringExtra("current_name") ?: ""

        setContent {
            EditTodoListScreen(listId, currentName) { newName ->
                if (newName.isNotBlank()) {
                    val dbHelper = TodoDatabaseHelper(this)
                    val db = dbHelper.readableDatabase


                    val cursor = db.rawQuery(
                        "SELECT COUNT(*) FROM ${TodoDatabaseHelper.TABLE_TODO_LIST} WHERE ${TodoDatabaseHelper.COLUMN_NAME} = ? AND ${TodoDatabaseHelper.COLUMN_ID} != ?",
                        arrayOf(newName, listId.toString())
                    )
                    cursor.moveToFirst()
                    val count = cursor.getInt(0)
                    cursor.close()

                    if (count > 0) {

                        Toast.makeText(this, "A Todo List with this name already exists", Toast.LENGTH_SHORT).show()
                    } else {

                        val values = ContentValues().apply {
                            put(TodoDatabaseHelper.COLUMN_NAME, newName)
                        }


                        val rowsAffected = db.update(
                            TodoDatabaseHelper.TABLE_TODO_LIST,
                            values,
                            "${TodoDatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(listId.toString())
                        )

                        db.close()

                        if (rowsAffected > 0) {
                            Toast.makeText(this, "Todo List Updated", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to Update Todo List", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Todo List Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun EditTodoListScreen(listId: Int, currentName: String, onSaveClick: (String) -> Unit) {
    var newName by remember { mutableStateOf(currentName) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Todo List Name") }
        )

        Button(
            onClick = {
                if (newName.isNotBlank()) {
                    onSaveClick(newName)
                } else {
                    Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save Changes")
        }
    }
}