package com.example.todofinal

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.*

class AddTodoItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listId = intent.getIntExtra("list_id", -1)
        if (listId == -1) {
            Toast.makeText(this, "Invalid List ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AddTodoItemScreen { itemName, dueDate ->
                if (itemName.isNotBlank()) {
                    val dbHelper = TodoDatabaseHelper(this)
                    val db = dbHelper.writableDatabase

                    val values = ContentValues().apply {
                        put(TodoDatabaseHelper.COLUMN_ITEM_NAME, itemName)
                        put(TodoDatabaseHelper.COLUMN_DUE_DATE, dueDate)
                        put(TodoDatabaseHelper.COLUMN_LIST_ID, listId)
                        put(TodoDatabaseHelper.COLUMN_COMPLETED, 0)
                    }

                    val result = db.insert(TodoDatabaseHelper.TABLE_TODO_ITEM, null, values)
                    db.close()

                    if (result != -1L) {
                        Toast.makeText(this, "Todo item added successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to add todo item", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Item Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


fun formatDateToString(date: Date): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return dateFormat.format(date)
}

@Composable
fun AddTodoItemScreen(onSaveClick: (String, String?) -> Unit) {
    val context = LocalContext.current
    var itemName by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<String?>(null) }
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)

            dueDate = formatDateToString(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = Calendar.getInstance().timeInMillis
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Name") }
        )
        OutlinedTextField(
            value = dueDate ?: "",
            onValueChange = {},
            label = { Text("Due Date (optional)") },
            enabled = false,
            modifier = Modifier.padding(top = 16.dp)
        )
        Button(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Select Due Date")
        }
        Button(
            onClick = { onSaveClick(itemName, dueDate) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save Item")
        }
    }
}
