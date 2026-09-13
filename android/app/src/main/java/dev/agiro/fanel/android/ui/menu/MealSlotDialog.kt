package dev.agiro.fanel.android.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.PopupProperties
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.data.remote.MealSlotDto
import dev.agiro.fanel.android.data.remote.MealType
import dev.agiro.fanel.android.data.remote.RecipeDto

@Composable
fun MealSlotDialog(
    dayLabel: String,
    mealType: MealType,
    slot: MealSlotDto?,
    recipes: List<RecipeDto>,
    onDismiss: () -> Unit,
    onSave: (text: String?, recipeId: String?) -> Unit,
    onClear: () -> Unit
) {
    val initial = slot?.recipeId?.let { id -> recipes.firstOrNull { it.id == id }?.name }
        ?: slot?.text
        ?: ""
    var value by remember { mutableStateOf(initial) }
    var focused by remember { mutableStateOf(false) }

    val suggestions = remember(value, recipes) {
        val q = value.trim()
        if (q.isEmpty() || recipes.any { it.name.equals(q, ignoreCase = true) }) {
            emptyList()
        } else {
            recipes.filter { it.name.contains(q, ignoreCase = true) }.take(5)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$dayLabel · ${stringResource(mealLabelRes(mealType))}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(stringResource(R.string.menu_slot_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused }
                    )
                    DropdownMenu(
                        expanded = focused && suggestions.isNotEmpty(),
                        onDismissRequest = { focused = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        suggestions.forEach { recipe ->
                            DropdownMenuItem(
                                text = { Text(recipe.name) },
                                onClick = { value = recipe.name }
                            )
                        }
                    }
                }
                if (slot?.recipeId != null) {
                    Text(
                        text = stringResource(R.string.menu_slot_linked_recipe),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = value.trim()
                    val match = recipes.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                    onSave(
                        if (match == null) trimmed else null,
                        match?.id
                    )
                },
                enabled = value.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            if (slot != null) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
