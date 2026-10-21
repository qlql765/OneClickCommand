package com.qile.oneclickcommand.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qile.oneclickcommand.data.AppDatabase
import com.qile.oneclickcommand.data.entity.ActionEntity
import com.qile.oneclickcommand.data.entity.CommandEntity
import com.qile.oneclickcommand.executor.CommandExecuteService
import com.qile.oneclickcommand.ui.theme.OneClickCommandTheme
import com.qile.oneclickcommand.util.ShortcutHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneClickCommandTheme {
                MainScreen(
                    viewModel = viewModel,
                    onAddCommand = {
                        startActivity(Intent(this, EditCommandActivity::class.java))
                    },
                    onEditCommand = { command ->
                        startActivity(
                            Intent(this, EditCommandActivity::class.java).apply {
                                putExtra("commandId", command.id)
                            }
                        )
                    },
                    onExecuteCommand = { command ->
                        CommandExecuteService.start(this, command.id, command.name)
                    },
                    onAddShortcut = { command ->
                        ShortcutHelper.createShortcut(this, command)
                    },
                    onDeleteCommand = { command ->
                        viewModel.deleteCommand(command) {
                            ShortcutHelper.removeShortcut(this, command.id)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddCommand: () -> Unit,
    onEditCommand: (CommandEntity) -> Unit,
    onExecuteCommand: (CommandEntity) -> Unit,
    onAddShortcut: (CommandEntity) -> Unit,
    onDeleteCommand: (CommandEntity) -> Unit
) {
    val commands by viewModel.commands.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "一键指令",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCommand,
                icon = { Icon(Icons.Default.Add, contentDescription = "新建") },
                text = { Text("新建指令") }
            )
        }
    ) { padding ->
        if (commands.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(commands, key = { it.id }) { command ->
                    CommandCard(
                        command = command,
                        onEdit = { onEditCommand(command) },
                        onExecute = { onExecuteCommand(command) },
                        onAddShortcut = { onAddShortcut(command) },
                        onDelete = { onDeleteCommand(command) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "📋",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "还没有指令",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "点击右下角按钮创建第一条指令",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CommandCard(
    command: CommandEntity,
    onEdit: () -> Unit,
    onExecute: () -> Unit,
    onAddShortcut: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onExecute,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 彩色圆点
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(command.iconColor)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    command.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ActionPreview(commandId = command.id)
            }
            // 菜单按钮
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "更多操作")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("添加到桌面") },
                        leadingIcon = { Icon(Icons.Default.IosShare, null) },
                        onClick = {
                            showMenu = false
                            onAddShortcut()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除指令") },
            text = { Text("确定要删除「${command.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun ActionPreview(commandId: Long) {
    val context = LocalContext.current
    var actionNames by remember { mutableStateOf(listOf<String>()) }
    androidx.compose.runtime.LaunchedEffect(commandId) {
        val db = AppDatabase.getDatabase(context)
        val actions = db.actionDao().getActionsByCommandId(commandId)
        actionNames = actions.map { it.appName }
    }
    if (actionNames.isNotEmpty()) {
        Text(
            actionNames.joinToString(" → "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    } else {
        Text(
            "无动作",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
