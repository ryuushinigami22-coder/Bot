package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * GalleryManager.kt
 * Access device gallery - photos and videos
 * Retrieve media files from device storage
 */
class GalleryManager(private val context: Context) {

    private val TAG = "GalleryManager"

    /**
     * Check if storage permission is granted
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get all photos
     */
    fun getAllPhotos(): List<MediaItem> {
        if (!hasPermission()) {
            Log.e(TAG, "Storage permission not granted")
            return emptyList()
        }

        val photos = mutableListOf<MediaItem>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (it.moveToNext()) {
                photos.add(
                    MediaItem(
                        id = it.getLong(idIndex),
                        name = it.getString(nameIndex) ?: "Unknown",
                        path = it.getString(pathIndex) ?: "",
                        size = it.getLong(sizeIndex),
                        dateAdded = it.getLong(dateIndex) * 1000,
                        width = it.getInt(widthIndex),
                        height = it.getInt(heightIndex),
                        type = MediaType.PHOTO
                    )
                )
            }
        }

        Log.d(TAG, "Retrieved ${photos.size} photos")
        return photos
    }

    /**
     * Get all videos
     */
    fun getAllVideos(): List<MediaItem> {
        if (!hasPermission()) {
            Log.e(TAG, "Storage permission not granted")
            return emptyList()
        }

        val videos = mutableListOf<MediaItem>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                videos.add(
                    MediaItem(
                        id = it.getLong(idIndex),
                        name = it.getString(nameIndex) ?: "Unknown",
                        path = it.getString(pathIndex) ?: "",
                        size = it.getLong(sizeIndex),
                        dateAdded = it.getLong(dateIndex) * 1000,
                        width = it.getInt(widthIndex),
                        height = it.getInt(heightIndex),
                        duration = it.getLong(durationIndex),
                        type = MediaType.VIDEO
                    )
                )
            }
        }

        Log.d(TAG, "Retrieved ${videos.size} videos")
        return videos
    }

    /**
     * Get all media (photos + videos)
     */
    fun getAllMedia(): List<MediaItem> {
        val photos = getAllPhotos()
        val videos = getAllVideos()
        return (photos + videos).sortedByDescending { it.dateAdded }
    }

    /**
     * Get recent media
     */
    fun getRecentMedia(limit: Int = 20): List<MediaItem> {
        return getAllMedia().take(limit)
    }

    /**
     * Get media by date range
     */
    fun getMediaByDateRange(startTime: Long, endTime: Long): List<MediaItem> {
        return getAllMedia().filter { it.dateAdded in startTime..endTime }
    }

    /**
     * Search media by name
     */
    fun searchMedia(query: String): List<MediaItem> {
        return getAllMedia().filter { 
            it.name.contains(query, ignoreCase = true) 
        }
    }

    /**
     * Get photos only
     */
    fun getPhotosOnly(): List<MediaItem> {
        return getAllMedia().filter { it.type == MediaType.PHOTO }
    }

    /**
     * Get videos only
     */
    fun getVideosOnly(): List<MediaItem> {
        return getAllMedia().filter { it.type == MediaType.VIDEO }
    }

    /**
     * Get total media count
     */
    fun getTotalMediaCount(): Int {
        return getAllMedia().size
    }

    /**
     * Get total storage used
     */
    fun getTotalStorageUsed(): Long {
        return getAllMedia().sumOf { it.size }
    }

    /**
     * Delete media file
     */
    fun deleteMedia(mediaId: Long, isVideo: Boolean): Boolean {
        return try {
            val uri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val deleted = context.contentResolver.delete(
                uri,
                "${MediaStore.Images.Media._ID} = ?",
                arrayOf(mediaId.toString())
            )
            
            Log.d(TAG, "Deleted media: $mediaId, rows: $deleted")
            deleted > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting media", e)
            false
        }
    }

    /**
     * Export media list as JSON
     */
    fun getMediaAsJson(): JSONArray {
        val media = getAllMedia()
        val jsonArray = JSONArray()

        media.forEach { item ->
            jsonArray.put(item.toJson())
        }

        return jsonArray
    }

    /**
     * Export media statistics
     */
    fun getMediaStatistics(): MediaStatistics {
        val allMedia = getAllMedia()
        val photos = allMedia.filter { it.type == MediaType.PHOTO }
        val videos = allMedia.filter { it.type == MediaType.VIDEO }
        
        return MediaStatistics(
            totalCount = allMedia.size,
            photoCount = photos.size,
            videoCount = videos.size,
            totalSize = allMedia.sumOf { it.size },
            photoSize = photos.sumOf { it.size },
            videoSize = videos.sumOf { it.size }
        )
    }

    companion object {
        /**
         * Format file size
         */
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }

        /**
         * Check if storage permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Media type enum
     */
    enum class MediaType {
        PHOTO,
        VIDEO
    }

    /**
     * Data class for media item
     */
    data class MediaItem(
        val id: Long,
        val name: String,
        val path: String,
        val size: Long,
        val dateAdded: Long,
        val width: Int,
        val height: Int,
        val duration: Long = 0,
        val type: MediaType
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("path", path)
                put("size", size)
                put("sizeFormatted", formatFileSize(size))
                put("dateAdded", dateAdded)
                put("dateFormatted", dateFormat.format(Date(dateAdded)))
                put("width", width)
                put("height", height)
                put("resolution", "${width}x${height}")
                if (type == MediaType.VIDEO) {
                    put("duration", duration)
                }
                put("type", type.name)
            }
        }
    }

    /**
     * Data class for media statistics
     */
    data class MediaStatistics(
        val totalCount: Int,
        val photoCount: Int,
        val videoCount: Int,
        val totalSize: Long,
        val photoSize: Long,
        val videoSize: Long
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("totalCount", totalCount)
                put("photoCount", photoCount)
                put("videoCount", videoCount)
                put("totalSize", totalSize)
                put("totalSizeFormatted", formatFileSize(totalSize))
                put("photoSize", photoSize)
                put("photoSizeFormatted", formatFileSize(photoSize))
                put("videoSize", videoSize)
                put("videoSizeFormatted", formatFileSize(videoSize))
            }
        }
    }
}
