package com.tbterminal.app.ui.products.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tbterminal.app.data.media.CloudinaryImageUrl
import com.tbterminal.app.data.media.ImageSize

@Composable
fun ProductPhotoThumbnail(
    photoUrl: String?,
    imageSize: ImageSize,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit
) {
    val resolvedModel = remember(photoUrl, imageSize) {
        val trimmed = photoUrl?.trim()?.takeIf(String::isNotEmpty) ?: return@remember null
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            CloudinaryImageUrl.transform(trimmed, imageSize)
        } else {
            trimmed
        }
    }

    if (resolvedModel == null) {
        Box(modifier = modifier) {
            fallback()
        }
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(resolvedModel)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(modifier = Modifier.fillMaxSize()) {
                    fallback()
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize()) {
                    fallback()
                }
            }
        )
    }
}
