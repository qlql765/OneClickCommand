package com.qile.oneclickcommand.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qile.oneclickcommand.data.AppDatabase
import com.qile.oneclickcommand.data.entity.ActionEntity
import com.qile.oneclickcommand.data.entity.CommandEntity
import com.qile.oneclickcommand.ui.theme.OneClickCommandTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditCommandActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val commandId = intent.getLongExtra("commandId", 0)
        setContent {
            OneClickCommandTheme {
                EditScreen(
                    commandId = commandId,
                    onBack = { finish() },
                    onSaved = { finish() }
                )
            }
        }
    }
}

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)

private val presetColors = listOf(
    0xFF6200EE.toInt(), 0xFFE91E63.toInt(), 0xFFF44336.toInt(),
    0xFFFF9800.toInt(), 0xFF4CAF50.toInt(), 0xFF2196F3.toInt(),
    0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFF795548.toInt(),
    0xFF607D8B.toInt(), 0xFFFFC107.toInt(), 0xFF3F51B5.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    commandId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(presetColors[0]) }
    var actions by remember { mutableStateOf(listOf<ActionEntity>()) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = commandId > 0

    // 加载已有指令
    LaunchedEffect(commandId) {
        if (isEditing) {
            val db = AppDatabase.getDatabase(context)
            val command = db.commandDao().getCommandById(commandId)
            command?.let {
                name = it.name
                selectedColor = it.iconColor
            }
            val loaded = db.actionDao().getActionsByCommandId(commandId)
                .sortedBy { it.orderIndex }
            actions = loaded
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "编辑指令" else "新建指令",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        when {
                            name.isBlank() -> errorMessage = "请输入指令名称"
                            actions.isEmpty() -> errorMessage = "请至少添加一个应用"
                            else -> {
                                errorMessage = null
                                scope.launch {
                                    saveCommand(context, commandId, name, selectedColor, actions)
                                    onSaved()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("保存", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 名称 + 颜色
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("指令名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor))
                            .clickable { showColorPicker = true }
                    )
                }
            )

            Spacer(Modifier.height(20.dp))

            // 应用列表标题 + 添加按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "应用列表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAppPicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("添加", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (actions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "点击「添加」选择要依次打开的应用",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(actions, key = { it.orderIndex }) { action ->
                        ActionItemRow(
                            action = action,
                            onDelete = {
                                actions = actions.minus(action).mapIndexed { i, a ->
                                    a.copy(orderIndex = i)
                                }
                            },
                            onMoveUp = {
                                val idx = actions.indexOf(action)
                                if (idx > 0) {
                                    val list = actions.toMutableList()
                                    list.removeAt(idx)
                                    list.add(idx - 1, action)
                                    actions = list.mapIndexed { i, a -> a.copy(orderIndex = i) }
                                }
                            },
                            onMoveDown = {
                                val idx = actions.indexOf(action)
                                if (idx < actions.size - 1) {
                                    val list = actions.toMutableList()
                                    list.removeAt(idx)
                                    list.add(idx + 1, action)
                                    actions = list.mapIndexed { i, a -> a.copy(orderIndex = i) }
                                }
                            }
                        )
                    }
                }
            }

            errorMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(msg, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                actions = actions + ActionEntity(
                    commandId = commandId,
                    orderIndex = actions.size,
                    packageName = app.packageName,
                    appName = app.appName
                )
                showAppPicker = false
            }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            selected = selectedColor,
            onDismiss = { showColorPicker = false },
            onSelected = { color ->
                selectedColor = color
                showColorPicker = false
            }
        )
    }
}

@Composable
fun ActionItemRow(
    action: ActionEntity,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(action.appName, fontWeight = FontWeight.Medium)
                Text(
                    action.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMoveUp) {
                Text("↑", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onMoveDown) {
                Text("↓", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (AppItem) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(listOf<AppItem>()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                .map {
                    AppItem(
                        packageName = it.activityInfo.packageName,
                        appName = it.loadLabel(pm).toString(),
                        icon = it.loadIcon(pm)
                    )
                }
                .sortedBy { it.appName.lowercase() }
        }
        apps = loaded
    }

    val filtered = if (query.isBlank()) apps else apps.filter {
        it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择应用") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    itemsIndexed(filtered) { _, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(drawable = app.icon)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.appName, fontWeight = FontWeight.Medium)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun AppIcon(drawable: Drawable) {
    val bitmap = remember(drawable) {
        val bmp = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(40.dp)
    )
}

@Composable
fun ColorPickerDialog(
    selected: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标颜色") },
        text = {
            Column {
                for (row in presetColors.chunked(6)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { color ->
                            val isSelected = color == selected
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable { onSelected(color) }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private suspend fun saveCommand(
    context: android.content.Context,
    commandId: Long,
    name: String,
    color: Int,
    actions: List<ActionEntity>
) {
    val db = AppDatabase.getDatabase(context)
    val now = System.currentTimeMillis()
    if (commandId > 0) {
        val existing = db.commandDao().getCommandById(commandId)
        val command = CommandEntity(
            id = commandId,
            name = name,
            iconColor = color,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        db.commandDao().updateCommand(command)
        db.actionDao().deleteActionsByCommandId(commandId)
        db.actionDao().insertActions(actions.map { it.copy(commandId = commandId) })
    } else {
        val command = CommandEntity(name = name, iconColor = color, createdAt = now, updatedAt = now)
        val id = db.commandDao().insertCommand(command)
        db.actionDao().insertActions(actions.map { it.copy(commandId = id) })
    }
}
