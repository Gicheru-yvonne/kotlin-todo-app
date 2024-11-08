package com.example.todofinal

import android.content.ContentValues
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

data class TodoItem(
    val id: Int,
    val name: String,
    val listId: String,
    val dueDate: String?,
    val isCompleted: Boolean
)

data class TodoListData(val id: Int, val name: String, val itemCount: Int, val completedCount: Int)

class MainActivity : ComponentActivity() {
    private var todoLists by mutableStateOf(listOf<TodoListData>())
    private var todoItems by mutableStateOf(listOf<TodoItem>())

    private lateinit var addListLauncher: ActivityResultLauncher<Intent>
    private lateinit var addItemLauncher: ActivityResultLauncher<Intent>
    private lateinit var editListLauncher: ActivityResultLauncher<Intent>
    private lateinit var editItemLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        editListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadTodoLists()
            }
        }

        editItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadTodoItems()
            }
        }

        setContent {
            TodoListApp(
                todoLists = todoLists,
                todoItems = todoItems,
                onAddTodoListClicked = {
                    val intent = Intent(this@MainActivity, AddTodoListActivity::class.java)
                    addListLauncher.launch(intent)
                },
                onAddTodoItemClicked = { listId ->
                    val intent = Intent(this@MainActivity, AddTodoItemActivity::class.java)
                    intent.putExtra("list_id", listId)
                    addItemLauncher.launch(intent)
                },
                onEditTodoListClicked = { listId, currentName ->
                    val intent = Intent(this@MainActivity, EditTodoListActivity::class.java)
                    intent.putExtra("list_id", listId)
                    intent.putExtra("current_name", currentName)
                    editListLauncher.launch(intent)
                },
                onEditTodoItemClicked = { itemId, itemName, dueDate ->
                    val intent = Intent(this@MainActivity, EditTodoItemActivity::class.java)
                    intent.putExtra("item_id", itemId)
                    intent.putExtra("item_name", itemName)
                    intent.putExtra("due_date", dueDate)
                    editItemLauncher.launch(intent)
                },
                onToggleItemCompletion = { itemId, isCompleted ->
                    toggleItemCompletion(itemId, isCompleted)
                }
            )
        }

        loadTodoLists()
        loadTodoItems()
    }

    internal fun toggleItemCompletion(itemId: Int, isCompleted: Boolean) {
        try {
            val dbHelper = TodoDatabaseHelper(this@MainActivity)
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put(TodoDatabaseHelper.COLUMN_COMPLETED, if (isCompleted) 1 else 0)
            }

            val rowsAffected = db.update(
                TodoDatabaseHelper.TABLE_TODO_ITEM,
                values,
                "${TodoDatabaseHelper.COLUMN_ID} = ?",
                arrayOf(itemId.toString())
            )
            db.close()

            if (rowsAffected > 0) {
                Log.d("MainActivity", "Updated item ID: $itemId, completed: $isCompleted")
            } else {
                Log.w("MainActivity", "No rows updated for item ID: $itemId")
            }

            loadTodoItems()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error toggling item completion: ${e.message}")
        }
    }

    internal fun loadTodoLists() {
        try {
            val dbHelper = TodoDatabaseHelper(this@MainActivity)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${TodoDatabaseHelper.COLUMN_ID}, ${TodoDatabaseHelper.COLUMN_NAME} FROM ${TodoDatabaseHelper.TABLE_TODO_LIST}",
                null
            )

            val lists = mutableListOf<TodoListData>()
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_NAME))
                    val counts = dbHelper.getItemCountsForList(id)
                    lists.add(TodoListData(id, name, counts.first, counts.second))
                }
                cursor.close()
            } else {
                Log.e("MainActivity", "Cursor is null while loading todo lists")
            }
            db.close()

            todoLists = lists
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading todo lists: ${e.message}")
        }
    }

    internal fun loadTodoItems() {
        try {
            val dbHelper = TodoDatabaseHelper(this@MainActivity)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT ${TodoDatabaseHelper.COLUMN_ID}, ${TodoDatabaseHelper.COLUMN_ITEM_NAME}, ${TodoDatabaseHelper.COLUMN_LIST_ID}, ${TodoDatabaseHelper.COLUMN_DUE_DATE}, ${TodoDatabaseHelper.COLUMN_COMPLETED} FROM ${TodoDatabaseHelper.TABLE_TODO_ITEM}",
                null
            )

            val items = mutableListOf<TodoItem>()
            if (cursor != null) {
                Log.d("MainActivity", "Cursor loaded with item count: ${cursor.count}")
                while (cursor.moveToNext()) {
                    val itemId = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ID))
                    val itemName = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ITEM_NAME)) ?: continue
                    val listId = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_LIST_ID)) ?: continue
                    val dueDate = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_DUE_DATE))
                    val isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_COMPLETED)) == 1

                    items.add(TodoItem(itemId, itemName, listId, dueDate, isCompleted))
                }
                cursor.close()
            } else {
                Log.e("MainActivity", "Cursor is null while loading todo items")
            }
            db.close()

            todoItems = items
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading todo items: ${e.message}")
        }
    }
}

@Composable
fun TodoListApp(
    todoLists: List<TodoListData>,
    todoItems: List<TodoItem>,
    onAddTodoListClicked: () -> Unit,
    onAddTodoItemClicked: (Int) -> Unit,
    onEditTodoListClicked: (Int, String) -> Unit,
    onEditTodoItemClicked: (Int, String, String?) -> Unit,
    onToggleItemCompletion: (Int, Boolean) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Todo Lists", style = MaterialTheme.typography.headlineMedium)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(todoLists) { list ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = list.name)
                            IconButton(onClick = { onEditTodoListClicked(list.id, list.name) }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit List",
                                    tint = Color(0xFFFFC0CB)
                                )
                            }
                        }
                        Text(text = "Items: ${list.itemCount}, Completed: ${list.completedCount}")

                        todoItems.filter { it.listId == list.id.toString() }.forEach { item ->
                            Row(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        onToggleItemCompletion(item.id, isChecked)
                                    }
                                )
                                Text(
                                    text = "${item.name} (Due: ${item.dueDate ?: "No due date"})",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                IconButton(onClick = {
                                    onEditTodoItemClicked(item.id, item.name, item.dueDate)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Item",
                                        tint = Color(0xFFFFC0CB)
                                    )
                                }
                            }
                        }

                        Button(onClick = { onAddTodoItemClicked(list.id) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Add Item")
                        }
                    }
                }
            }

            Button(onClick = onAddTodoListClicked, modifier = Modifier.padding(top = 16.dp)) {
                Text("Add New Todo List")
            }
        }
    }
}
