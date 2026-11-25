package com.isabelle.assistant.photos

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Classifica as fotos NO APARELHO (ML Kit, sem enviar nada para a internet),
 * move para Pictures/Isabelle/<categoria> e sugere exclusões.
 * Mover e apagar exigem a sua confirmação no diálogo do Android (regra do sistema).
 */
object PhotoOrganizer {

    data class Photo(
        val uri: Uri, val name: String, val path: String,
        val size: Long, val takenMs: Long, var category: String = "Outros"
    )

    data class Scan(
        val photos: List<Photo>,
        val byCategory: Map<String, List<Photo>>,
        val deleteCandidates: List<Photo>
    )

    private val rules: List<Pair<String, List<String>>> = listOf(
        "Documentos" to listOf("Text", "Paper", "Document", "Font", "Handwriting", "Receipt", "Poster"),
        "Comida" to listOf("Food", "Dish", "Cuisine", "Meal", "Drink", "Fruit", "Pizza", "Cake", "Dessert"),
        "Animais" to listOf("Dog", "Cat", "Pet", "Bird", "Animal", "Fish", "Horse"),
        "Pessoas" to listOf("Selfie", "Smile", "Face", "Person", "Crowd", "Hair", "Beard", "Skin"),
        "Natureza" to listOf("Sky", "Flower", "Tree", "Plant", "Mountain", "Beach", "Sea", "Sunset", "Cloud", "Lake"),
        "Veiculos" to listOf("Car", "Vehicle", "Motorcycle", "Bicycle", "Truck", "Wheel"),
        "Lugares" to listOf("Building", "City", "House", "Tower", "Bridge", "Street", "Architecture")
    )

    private fun categorize(labels: List<String>): String {
        for ((cat, keys) in rules) if (labels.any { it in keys }) return cat
        return "Outros"
    }

    suspend fun scan(c: Context, limit: Int = 400): Scan = withContext(Dispatchers.IO) {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED
        )
        val args = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaStore.Images.Media.DATE_ADDED} DESC")
        }
        val photos = mutableListOf<Photo>()
        c.contentResolver.query(collection, proj, args, null)?.use { cur ->
            while (cur.moveToNext()) {
                val path = cur.getString(2) ?: ""
                if (path.startsWith("Pictures/Isabelle/")) continue // já organizada
                val taken = if (cur.isNull(4)) cur.getLong(5) * 1000 else cur.getLong(4)
                photos += Photo(
                    ContentUris.withAppendedId(collection, cur.getLong(0)),
                    cur.getString(1) ?: "", path, cur.getLong(3), taken
                )
            }
        }

        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.6f).build()
        )
        for (p in photos) {
            p.category = if (p.path.contains("Screenshots", true) || p.name.startsWith("Screenshot", true)) {
                "Capturas de tela"
            } else try {
                val bmp = c.contentResolver.loadThumbnail(p.uri, Size(224, 224), null)
                categorize(labeler.process(InputImage.fromBitmap(bmp, 0)).await().map { it.text })
            } catch (_: Exception) { "Outros" }
        }
        labeler.close()

        val monthAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
        val oldShots = photos.filter { it.category == "Capturas de tela" && it.takenMs < monthAgo }
        val dupes = photos.groupBy { it.size to it.takenMs }.values.filter { it.size > 1 }.flatMap { it.drop(1) }
        val candidates = (oldShots + dupes).distinctBy { it.uri }

        Scan(photos, photos.groupBy { it.category }, candidates)
    }

    fun moveRequest(c: Context, photos: List<Photo>): IntentSender =
        MediaStore.createWriteRequest(c.contentResolver, photos.map { it.uri }).intentSender

    fun deleteRequest(c: Context, photos: List<Photo>): IntentSender =
        MediaStore.createDeleteRequest(c.contentResolver, photos.map { it.uri }).intentSender

    /** Chamar depois que o usuário aprovou moveRequest. */
    suspend fun applyMove(c: Context, photos: List<Photo>) = withContext(Dispatchers.IO) {
        for (p in photos) {
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Isabelle/${p.category}/")
            }
            try { c.contentResolver.update(p.uri, v, null, null) } catch (_: Exception) {}
        }
    }
}
