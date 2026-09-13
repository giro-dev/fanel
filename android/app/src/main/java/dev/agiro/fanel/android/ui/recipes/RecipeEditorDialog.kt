package dev.agiro.fanel.android.ui.recipes

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.agiro.fanel.android.R
import dev.agiro.fanel.android.data.IngredientDraft
import dev.agiro.fanel.android.data.RecipeDraft

private data class IngredientInput(
    val name: String = "",
    val quantity: String = "",
    val unit: String = ""
)

private data class ImageInput(val mimeType: String, val data: String)

@Composable
fun RecipeEditorDialog(onDismiss: () -> Unit, onConfirm: (RecipeDraft) -> Unit) {
    var name by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("4") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf("") }
    val ingredients = remember { mutableStateListOf(IngredientInput()) }
    var image by remember { mutableStateOf<ImageInput?>(null) }
    var error by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                if (bytes != null) {
                    image = ImageInput(mimeType, Base64.encodeToString(bytes, Base64.DEFAULT))
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recipe_new)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = false },
                    label = { Text(stringResource(R.string.recipe_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = servingsText,
                    onValueChange = { servingsText = it.filter(Char::isDigit); error = false },
                    label = { Text(stringResource(R.string.recipe_servings_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.recipe_description)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.recipe_notes)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.recipe_steps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                steps.forEachIndexed { index, step ->
                    OutlinedTextField(
                        value = step,
                        onValueChange = { steps[index] = it },
                        label = { Text(stringResource(R.string.recipe_step_n, index + 1)) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                TextButton(onClick = { steps.add("") }) {
                    Text(stringResource(R.string.recipe_add_step))
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.recipe_ingredients),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ingredients.forEachIndexed { index, ingredient ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = ingredient.name,
                            onValueChange = { ingredients[index] = ingredient.copy(name = it) },
                            label = { Text(stringResource(R.string.recipe_ingredient_name)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = ingredient.quantity,
                            onValueChange = {
                                ingredients[index] = ingredient.copy(
                                    quantity = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                                )
                            },
                            label = { Text(stringResource(R.string.recipe_quantity)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = ingredient.unit,
                            onValueChange = { ingredients[index] = ingredient.copy(unit = it) },
                            label = { Text(stringResource(R.string.recipe_unit)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TextButton(onClick = { ingredients.add(IngredientInput()) }) {
                    Text(stringResource(R.string.recipe_add_ingredient))
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                    Text(
                        stringResource(
                            if (image == null) R.string.recipe_pick_image
                            else R.string.recipe_image_selected
                        )
                    )
                }

                if (error) {
                    Text(
                        text = stringResource(R.string.recipe_invalid_input),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val servings = servingsText.toIntOrNull()
                    if (name.isBlank() || servings == null || servings < 1) {
                        error = true
                    } else {
                        onConfirm(
                            RecipeDraft(
                                name = name.trim(),
                                servings = servings,
                                notes = notes.trim().ifBlank { null },
                                description = description.trim().ifBlank { null },
                                steps = steps.map { it.trim() }.filter { it.isNotEmpty() },
                                ingredients = ingredients
                                    .filter { it.name.isNotBlank() }
                                    .map {
                                        IngredientDraft(
                                            name = it.name.trim(),
                                            quantity = it.quantity
                                                .replace(',', '.')
                                                .toDoubleOrNull(),
                                            unit = it.unit.trim().ifBlank { null }
                                        )
                                    },
                                imageMimeType = image?.mimeType,
                                imageData = image?.data
                            )
                        )
                    }
                }
            ) {
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
