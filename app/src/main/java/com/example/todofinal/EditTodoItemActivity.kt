package com.example.todofinal

import android.app.DatePickerDialog
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
import java.util.*

class EditTodoItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getIntExtra("item_id", -1)
        val itemName = intent.getStringExtra("item_name") ?: ""
        val dueDate = intent.getStringExtra("due_date") ?: ""

        setContent {
            EditTodoItemScreen(itemName, dueDate) { newItemName, newDueDate ->
                if (newItemName.isNotBlank()) {
                    val dbHelper = TodoDatabaseHelper(this)
                    val success = dbHelper.updateTodoItem(itemId, newItemName, newDueDate)

                    if (success) {
                        Toast.makeText(this, "Todo Item Updated", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to Update Todo Item", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Item name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun EditTodoItemScreen(
    itemName: String,
    currentDueDate: String,
    onSaveClick: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var newItemName by remember { mutableStateOf(itemName) }
    var dueDate by remember { mutableStateOf(currentDueDate) }
    val calendar = Calendar.getInstance()

    if (currentDueDate.isNotBlank()) {
        val parts = currentDueDate.split("-")
        if (parts.size == 3) {
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
    }

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
            value = newItemName,
            onValueChange = { newItemName = it },
            label = { Text("Todo Item Name") }
        )

        OutlinedTextField(
            value = dueDate,
            onValueChange = {},
            label = { Text("Due Date (Optional)") },
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
            onClick = {
                onSaveClick(newItemName, dueDate)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save Todo Item")
        }
    }
}
