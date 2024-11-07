package com.example.todofinal

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

data class TodoItem(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadTodoLists()
                loadTodoItems()
            }
        }

        addItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadTodoItems()
                loadTodoLists()
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
                onToggleItemCompletion = { itemName, listId, isCompleted ->
                    toggleItemCompletion(itemName, listId, isCompleted)
                }
            )
        }

        loadTodoLists()
        loadTodoItems()
    }

    internal fun toggleItemCompletion(itemName: String, listId: String, isCompleted: Boolean) {
        val dbHelper = TodoDatabaseHelper(this@MainActivity)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(TodoDatabaseHelper.COLUMN_COMPLETED, if (isCompleted) 1 else 0)
        }

        db.update(
            TodoDatabaseHelper.TABLE_TODO_ITEM,
            values,
            "${TodoDatabaseHelper.COLUMN_ITEM_NAME} = ? AND ${TodoDatabaseHelper.COLUMN_LIST_ID} = ?",
            arrayOf(itemName, listId)
        )
        db.close()

        loadTodoItems()
    }

    internal fun loadTodoLists() {
        val dbHelper = TodoDatabaseHelper(this@MainActivity)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${TodoDatabaseHelper.COLUMN_ID}, ${TodoDatabaseHelper.COLUMN_NAME} FROM ${TodoDatabaseHelper.TABLE_TODO_LIST}",
            null
        )

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

    internal fun loadTodoItems() {
        val dbHelper = TodoDatabaseHelper(this@MainActivity)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${TodoDatabaseHelper.COLUMN_ITEM_NAME}, ${TodoDatabaseHelper.COLUMN_LIST_ID}, ${TodoDatabaseHelper.COLUMN_DUE_DATE}, ${TodoDatabaseHelper.COLUMN_COMPLETED} FROM ${TodoDatabaseHelper.TABLE_TODO_ITEM}",
            null
        )

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
    onAddTodoItemClicked: (Int) -> Unit,
    onToggleItemCompletion: (String, String, Boolean) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Todo Lists", style = MaterialTheme.typography.headlineMedium)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(todoLists) { list ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = list.name)
                        Text(text = "Items: ${list.itemCount}, Completed: ${list.completedCount}")

                        todoItems.filter { it.listId == list.id.toString() }.forEach { item ->
                            Row(modifier = Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        onToggleItemCompletion(item.name, item.listId, isChecked)
                                    }
                                )
                                Text(
                                    text = "${item.name} (Due: ${item.dueDate ?: "No due date"})",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
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
