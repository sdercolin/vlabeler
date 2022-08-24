package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.sdercolin.vlabeler.env.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun <T> AsyncImage(
    load: suspend () -> T,
    painterFor: @Composable (T) -> Painter,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    vararg keys: Any,
) {
    val image: T? by produceState<T?>(null, *keys) {
        value = withContext(Dispatchers.IO) {
            try {
                load()
            } catch (t: Throwable) {
                Log.error(t)
                null
            }
        }
    }

    key(image) {
        image?.let {
            Image(
                painter = painterFor(it),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier,
            )
        }
    }
}
