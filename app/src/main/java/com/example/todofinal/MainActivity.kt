package com.example.todofinal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

data class TodoListData(val id: Int, val name: String)

class MainActivity : ComponentActivity() {
    private var todoLists by mutableStateOf(listOf<TodoListData>())
    private lateinit var addListLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("MainActivity", "Received RESULT_OK from AddTodoListActivity, reloading data")
                loadTodoLists()
            }
        }

        setContent {
            TodoListApp(
                todoLists = todoLists,
                onAddTodoListClicked = {
                    val intent = Intent(this, AddTodoListActivity::class.java)
                    addListLauncher.launch(intent)
                }
            )
        }

        loadTodoLists()
    }

    internal fun loadTodoLists() {
        val dbHelper = TodoDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT ${TodoDatabaseHelper.COLUMN_ID}, ${TodoDatabaseHelper.COLUMN_NAME} FROM ${TodoDatabaseHelper.TABLE_TODO_LIST}", null)

        val lists = mutableListOf<TodoListData>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_NAME))
            lists.add(TodoListData(id, name))
        }
        cursor.close()
        db.close()

        todoLists = lists
    }
}

@Composable
fun TodoListApp(
    todoLists: List<TodoListData>,
    onAddTodoListClicked: () -> Unit
) {
    val context = LocalContext.current

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Todo Lists", style = MaterialTheme.typography.headlineMedium)

            if (todoLists.isNotEmpty()) {
                todoLists.forEach { list ->
                    Text(text = list.name, modifier = Modifier.padding(8.dp))
                }
            } else {
                Text(text = "No todo lists yet.")
            }

            Button(onClick = { onAddTodoListClicked() }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Add New Todo List")
            }
        }
    }
}
