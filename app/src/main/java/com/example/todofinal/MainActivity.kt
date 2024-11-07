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
import com.example.todofinal.com.example.todofinal.AddTodoItemActivity

// Define TodoItem and TodoListData data classes
data class TodoItem(val name: String, val listId: String, val dueDate: String?, val isCompleted: Boolean)
data class TodoListData(val id: Int, val name: String, val itemCount: Int, val completedCount: Int)

class MainActivity : ComponentActivity() {
    private var todoLists by mutableStateOf(listOf<TodoListData>())
    private var todoItems by mutableStateOf(listOf<TodoItem>())

    private lateinit var addListLauncher: ActivityResultLauncher<Intent>
    private lateinit var addItemLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the launchers for adding a new list and items
        addListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadTodoLists()
            }
        }
        addItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadTodoItems()
            }
        }

        setContent {
            TodoListApp(
                todoLists = todoLists,
                todoItems = todoItems,
                onAddTodoListClicked = {
                    val intent = Intent(this, AddTodoListActivity::class.java)
                    addListLauncher.launch(intent)
                },
                onAddTodoItemClicked = { listId ->
                    val intent = Intent(this, AddTodoItemActivity::class.java)
                    intent.putExtra("list_id", listId)
                    addItemLauncher.launch(intent)
                }
            )
        }

        loadTodoLists()
        loadTodoItems()
    }

    // Load todo lists from the database
    internal fun loadTodoLists() {
        val dbHelper = TodoDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT ${TodoDatabaseHelper.COLUMN_ID}, ${TodoDatabaseHelper.COLUMN_NAME} FROM ${TodoDatabaseHelper.TABLE_TODO_LIST}", null)

        val lists = mutableListOf<TodoListData>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_NAME))
            val counts = dbHelper.getItemCountsForList(id)
            lists.add(TodoListData(id, name, counts.first, counts.second))
        }
        cursor.close()
        db.close()

        todoLists = lists
    }

    // Load todo items from the database
    internal fun loadTodoItems() {
        val dbHelper = TodoDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT ${TodoDatabaseHelper.COLUMN_ITEM_NAME}, ${TodoDatabaseHelper.COLUMN_LIST_ID}, ${TodoDatabaseHelper.COLUMN_DUE_DATE}, ${TodoDatabaseHelper.COLUMN_COMPLETED} FROM ${TodoDatabaseHelper.TABLE_TODO_ITEM}", null)

        val items = mutableListOf<TodoItem>()
        while (cursor.moveToNext()) {
            val itemName = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ITEM_NAME))
            val listId = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_LIST_ID))
            val dueDate = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_DUE_DATE))
            val isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_COMPLETED)) == 1
            items.add(TodoItem(itemName, listId, dueDate, isCompleted))
        }
        cursor.close()
        db.close()

        todoItems = items
    }
}

@Composable
fun TodoListApp(
    todoLists: List<TodoListData>,
    todoItems: List<TodoItem>,
    onAddTodoListClicked: () -> Unit,
    onAddTodoItemClicked: (Int) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Todo Lists", style = MaterialTheme.typography.headlineMedium)

            if (todoLists.isNotEmpty()) {
                todoLists.forEach { list ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = list.name)
                        Text(text = "Items: ${list.itemCount}, Completed: ${list.completedCount}")
                        Button(onClick = { onAddTodoItemClicked(list.id) }) {
                            Text("Add Item")
                        }
                    }
                }
            } else {
                Text(text = "No todo lists yet.")
            }

            Button(onClick = onAddTodoListClicked, modifier = Modifier.padding(top = 16.dp)) {
                Text("Add New Todo List")
            }
        }
    }
}
