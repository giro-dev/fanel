package dev.agiro.fanel.android.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.ShoppingViewModel
import dev.agiro.fanel.android.data.remote.AddItemRequest
import dev.agiro.fanel.android.data.remote.ShoppingItemDto
import dev.agiro.fanel.android.ui.components.OfflineBanner
import java.util.Locale
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var quickAddName by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    val activeList = state.activeList
    val quickAdd: () -> Unit = {
        val listId = activeList?.id
        val name = quickAddName.trim()
        if (listId != null && name.isNotEmpty()) {
            viewModel.addItem(listId, AddItemRequest(name, null, null, null, false))
            quickAddName = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_screen_title)) },
                actions = {
                    if (activeList != null && state.hasPurchased) {
                        IconButton(onClick = { viewModel.clearPurchased(activeList.id) }) {
                            Icon(
                                Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.shopping_clear_purchased)
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleGroupByCategory() }) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.shopping_group_by_category),
                            tint = if (state.groupByCategory) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.sync_now))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OfflineBanner()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.lists, key = { it.id }) { list ->
                    FilterChip(
                        selected = list.id == activeList?.id,
                        onClick = { viewModel.selectList(list.id) },
                        label = { Text("${list.name} (${state.pendingCount(list)})") }
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showCreateListDialog = true },
                        label = { Text("+ ${stringResource(R.string.shopping_new_list)}") }
                    )
                }
                if (activeList != null && state.lists.size > 1) {
                    item {
                        Row {
                            IconButton(onClick = { renameTarget = activeList.id }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.shopping_rename_list),
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            IconButton(onClick = { deleteTarget = activeList.id }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.shopping_delete_list_title),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (activeList != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickAddName,
                        onValueChange = { quickAddName = it },
                        label = { Text(stringResource(R.string.shopping_add_item)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { quickAdd() }),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.shopping_item_details)
                        )
                    }
                    IconButton(onClick = quickAdd, enabled = quickAddName.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.shopping_add_item)
                        )
                    }
                }
            }

            if (state.householdId.isBlank()) {
                Text(
                    stringResource(R.string.household_unconfigured_status),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.errorRes?.let { errorRes ->
                Text(
                    stringResource(errorRes),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val items = activeList?.items.orEmpty()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (state.groupByCategory) {
                    state.groups.forEach { group ->
                        item(key = "header-${group.name}") {
                            Text(
                                "${group.name} · ${group.items.count { !it.done }}/${group.items.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(group.items, key = { it.id }) { item ->
                            ShoppingItemRow(
                                item = item,
                                showCategory = false,
                                onToggleDone = { viewModel.toggleDone(item) },
                                onToggleRecurring = { viewModel.toggleRecurring(item) },
                                onRemove = { viewModel.removeItem(item) }
                            )
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            showCategory = true,
                            onToggleDone = { viewModel.toggleDone(item) },
                            onToggleRecurring = { viewModel.toggleRecurring(item) },
                            onRemove = { viewModel.removeItem(item) }
                        )
                    }
                }
                if (items.isEmpty() && activeList != null && !state.loading) {
                    item {
                        Text(
                            stringResource(R.string.shopping_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog && activeList != null) {
        AddItemDialog(
            initialName = quickAddName,
            categories = activeList.items.orEmpty()
                .mapNotNull { it.category?.trim()?.ifEmpty { null } }
                .distinct(),
            onDismiss = { showAddDialog = false },
            onConfirm = { request ->
                viewModel.addItem(activeList.id, request)
                quickAddName = ""
                showAddDialog = false
            }
        )
    }

    if (showCreateListDialog) {
        ListNameDialog(
            title = stringResource(R.string.shopping_new_list),
            initial = "",
            onDismiss = { showCreateListDialog = false },
            onConfirm = { name ->
                viewModel.createList(name)
                showCreateListDialog = false
            }
        )
    }

    renameTarget?.let { listId ->
        val list = state.lists.firstOrNull { it.id == listId }
        if (list != null) {
            ListNameDialog(
                title = stringResource(R.string.shopping_rename_list),
                initial = list.name,
                onDismiss = { renameTarget = null },
                onConfirm = { name ->
                    viewModel.renameList(listId, name)
                    renameTarget = null
                }
            )
        }
    }

    deleteTarget?.let { listId ->
        val list = state.lists.firstOrNull { it.id == listId }
        if (list != null) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text(stringResource(R.string.shopping_delete_list_title)) },
                text = { Text(stringResource(R.string.shopping_delete_list_message, list.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteList(listId)
                        deleteTarget = null
                    }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemDto,
    showCategory: Boolean,
    onToggleDone: () -> Unit,
    onToggleRecurring: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggleDone() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
            )
            val detail = listOfNotNull(
                item.quantity?.let { qty -> formatQuantity(qty) },
                item.unit?.takeIf { it.isNotBlank() },
                if (showCategory) item.category?.takeIf { it.isNotBlank() } else null
            ).joinToString(" ")
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onToggleRecurring) {
            Icon(
                Icons.Filled.Repeat,
                contentDescription = stringResource(R.string.shopping_recurring),
                tint = if (item.recurring) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ListNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.shopping_list_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatQuantity(quantity: Double): String =
    if (quantity == floor(quantity)) {
        String.format(Locale.getDefault(), "%d", quantity.toLong())
    } else {
        String.format(Locale.getDefault(), "%.2f", quantity).trimEnd('0').trimEnd('.', ',')
    }
