package dev.agiro.fanel.android.ui.recipes

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.agiro.fanel.android.data.remote.RecipeDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecipeImage(
    recipe: RecipeDto,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val data = recipe.imageData
    if (recipe.imageMimeType == null || data == null) return
    val bitmap by produceState<ImageBitmap?>(initialValue = null, data) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val bytes = Base64.decode(data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()?.asImageBitmap()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = recipe.name,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
