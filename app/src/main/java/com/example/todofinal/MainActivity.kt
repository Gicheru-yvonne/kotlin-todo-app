package com.example.todofinal

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

data class TodoItem(
    val id: Int,
    val name: String,
    val listId: String,
    val dueDate: String?,
    val isCompleted: Boolean
)

data class TodoListData(
    val id: Int,
    val name: String,
    val itemCount: Int,
    val completedCount: Int,
    val nearestDueDate: String?,
    val hasOverdueItems: Boolean = false,
    val hasItemsDueToday: Boolean = false
)

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
                loadTodoLists()
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
                onDeleteTodoItemClicked = { itemId ->
                    deleteTodoItem(itemId)
                },
                onToggleItemCompletion = { itemId, isCompleted ->
                    toggleItemCompletion(itemId, isCompleted)
                },
                onMoveTodoItemClicked = { itemId, targetListId ->
                    moveTodoItem(itemId, targetListId)
                }
            )
        }

        loadTodoLists()
        loadTodoItems()
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
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_NAME))
                val counts = dbHelper.getItemCountsForList(id)
                val nearestDueDate = dbHelper.getNearestDueDateForList(id)


                val hasOverdueItems = dbHelper.hasOverdueItems(id)
                val hasItemsDueToday = dbHelper.hasItemDueToday(id)


                val displayDueDate = nearestDueDate ?: "None"

                lists.add(
                    TodoListData(
                        id,
                        name,
                        counts.first,
                        counts.second,
                        displayDueDate,
                        hasOverdueItems,
                        hasItemsDueToday
                    )
                )
            }
            cursor.close()
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
            while (cursor.moveToNext()) {
                val itemId = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ID))
                val itemName = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_ITEM_NAME))
                val listId = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_LIST_ID))
                val dueDate = cursor.getString(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_DUE_DATE))
                val isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(TodoDatabaseHelper.COLUMN_COMPLETED)) == 1

                items.add(TodoItem(itemId, itemName, listId, dueDate, isCompleted))
            }
            cursor.close()
            db.close()

            todoItems = items
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading todo items: ${e.message}")
        }
    }

    internal fun deleteTodoItem(itemId: Int) {
        try {
            val dbHelper = TodoDatabaseHelper(this@MainActivity)
            val db = dbHelper.writableDatabase

            val rowsDeleted = db.delete(
                TodoDatabaseHelper.TABLE_TODO_ITEM,
                "${TodoDatabaseHelper.COLUMN_ID} = ?",
                arrayOf(itemId.toString())
            )
            db.close()

            if (rowsDeleted > 0) {
                Log.d("MainActivity", "Deleted item ID: $itemId")
            } else {
                Log.w("MainActivity", "No item deleted for ID: $itemId")
            }


            loadTodoItems()
            loadTodoLists()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error deleting item: ${e.message}")
        }
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
            loadTodoLists()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error toggling item completion: ${e.message}")
        }
    }

    internal fun moveTodoItem(itemId: Int, targetListId: Int) {
        try {
            val dbHelper = TodoDatabaseHelper(this@MainActivity)
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put(TodoDatabaseHelper.COLUMN_LIST_ID, targetListId)
            }

            val rowsAffected = db.update(
                TodoDatabaseHelper.TABLE_TODO_ITEM,
                values,
                "${TodoDatabaseHelper.COLUMN_ID} = ?",
                arrayOf(itemId.toString())
            )
            db.close()

            if (rowsAffected > 0) {
                Log.d("MainActivity", "Moved item ID: $itemId to list ID: $targetListId")
            } else {
                Log.w("MainActivity", "No rows updated for item ID: $itemId")
            }


            loadTodoItems()
            loadTodoLists()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error moving item: ${e.message}")
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
    onDeleteTodoItemClicked: (Int) -> Unit,
    onToggleItemCompletion: (Int, Boolean) -> Unit,
    onMoveTodoItemClicked: (Int, Int) -> Unit
) {
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf(0) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Todo Lists", style = MaterialTheme.typography.headlineMedium)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(todoLists) { list ->
                    val listBackgroundColor = when {
                        list.hasOverdueItems -> Color(0xFFFF7F7F)
                        list.hasItemsDueToday -> Color.Yellow
                        else -> Color.White
                    }

                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .background(listBackgroundColor)
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = list.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(onClick = { onEditTodoListClicked(list.id, list.name) }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit List",
                                    tint = Color(0xFFFFC0CB)
                                )
                            }
                        }

                        Text(
                            text = "Nearest Due Date: ${list.nearestDueDate}",
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(text = "${list.completedCount}/${list.itemCount}")

                        todoItems.filter { it.listId == list.id.toString() }.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = item.name,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                item.dueDate?.let { dueDate ->
                                    Text(
                                        text = "Due: $dueDate",
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            onToggleItemCompletion(item.id, isChecked)
                                        }
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            onEditTodoItemClicked(item.id, item.name, item.dueDate)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit Item",
                                                tint = Color(0xFFFFC0CB)
                                            )
                                        }
                                        IconButton(onClick = {
                                            onDeleteTodoItemClicked(item.id)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete Item",
                                                tint = Color.Red
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                showMoveDialog = true
                                                itemToMove = item.id
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFC0CB),
                                                contentColor = Color.Black
                                            ),
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .width(80.dp)
                                                .height(40.dp)
                                        ) {
                                            Text("Move")
                                        }
                                    }
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

    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move Item") },
            text = {
                Column {
                    Text("Select the list to move item to:")
                    Spacer(modifier = Modifier.height(8.dp))
                    todoLists.filter { it.id != todoItems.find { it.id == itemToMove }?.listId?.toInt() }.forEach { targetList ->
                        Button(onClick = {
                            onMoveTodoItemClicked(itemToMove, targetList.id)
                            showMoveDialog = false
                        }, modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth()) {
                            Text(targetList.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
