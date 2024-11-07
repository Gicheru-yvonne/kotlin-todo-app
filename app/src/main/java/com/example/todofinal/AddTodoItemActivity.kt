package com.example.todofinal.com.example.todofinal

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
import com.example.todofinal.TodoDatabaseHelper

class AddTodoItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listId = intent.getIntExtra("list_id", -1)

        setContent {
            AddTodoItemScreen { itemName, dueDate ->
                if (itemName.isNotBlank()) {
                    val dbHelper = TodoDatabaseHelper(this)
                    val values = ContentValues().apply {
                        put(TodoDatabaseHelper.COLUMN_ITEM_NAME, itemName)
                        put(TodoDatabaseHelper.COLUMN_DUE_DATE, dueDate)
                        put(TodoDatabaseHelper.COLUMN_LIST_ID, listId)
                        put(TodoDatabaseHelper.COLUMN_COMPLETED, 0)
                    }
                    val result = dbHelper.writableDatabase.insert(TodoDatabaseHelper.TABLE_TODO_ITEM, null, values)
                    if (result != -1L) {
                        Toast.makeText(this, "Todo Item Added", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to Add Todo Item", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Item Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun AddTodoItemScreen(onSaveClick: (String, String) -> Unit) {
    var itemName by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Name") }
        )
        OutlinedTextField(
            value = dueDate,
            onValueChange = { dueDate = it },
            label = { Text("Due Date (optional)") }
        )
        Button(
            onClick = { onSaveClick(itemName, dueDate) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save Item")
        }
    }
}
