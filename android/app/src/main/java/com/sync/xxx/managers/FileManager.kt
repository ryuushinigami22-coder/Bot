package com.sync.xxx.managers

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * FileManager.kt
 * File system operations - list, read, write, delete, rename
 * Navigate and manage device files
 */
class FileManager(private val context: Context) {

    private val TAG = "FileManager"

    /**
     * List files in directory
     */
    fun listFiles(path: String): List<FileInfo> {
        return try {
            val directory = File(path)
            
            if (!directory.exists()) {
                Log.e(TAG, "Directory does not exist: $path")
                return emptyList()
            }
            
            if (!directory.isDirectory) {
                Log.e(TAG, "Path is not a directory: $path")
                return emptyList()
            }
            
            val files = directory.listFiles() ?: emptyArray()
            files.map { file ->
                FileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    size = if (file.isFile) file.length() else getFolderSize(file),
                    isDirectory = file.isDirectory,
                    lastModified = file.lastModified(),
                    canRead = file.canRead(),
                    canWrite = file.canWrite()
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.toLowerCase() }))
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            emptyList()
        }
    }

    /**
     * Get file info
     */
    fun getFileInfo(path: String): FileInfo? {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return null
            }
            
            FileInfo(
                name = file.name,
                path = file.absolutePath,
                size = if (file.isFile) file.length() else getFolderSize(file),
                isDirectory = file.isDirectory,
                lastModified = file.lastModified(),
                canRead = file.canRead(),
                canWrite = file.canWrite()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file info", e)
            null
        }
    }

    /**
     * Read file content
     */
    fun readFile(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return null
            }
            
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file", e)
            null
        }
    }

    /**
     * Write file content
     */
    fun writeFile(path: String, content: String): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            Log.d(TAG, "File written: $path")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing file", e)
            false
        }
    }

    /**
     * Delete file or directory
     */
    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return false
            }
            
            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            
            if (deleted) {
                Log.d(TAG, "Deleted: $path")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }

    /**
     * Rename file or directory
     */
    fun renameFile(oldPath: String, newName: String): Boolean {
        return try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) {
                return false
            }
            
            val newFile = File(oldFile.parent, newName)
            val renamed = oldFile.renameTo(newFile)
            
            if (renamed) {
                Log.d(TAG, "Renamed: $oldPath -> ${newFile.absolutePath}")
            }
            renamed
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming file", e)
            false
        }
    }

    /**
     * Copy file
     */
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            
            if (!sourceFile.exists() || !sourceFile.isFile) {
                return false
            }
            
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)
            Log.d(TAG, "Copied: $sourcePath -> $destPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file", e)
            false
        }
    }

    /**
     * Move file
     */
    fun moveFile(sourcePath: String, destPath: String): Boolean {
        return try {
            if (copyFile(sourcePath, destPath)) {
                deleteFile(sourcePath)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error moving file", e)
            false
        }
    }

    /**
     * Create directory
     */
    fun createDirectory(path: String): Boolean {
        return try {
            val directory = File(path)
            val created = directory.mkdirs()
            if (created) {
                Log.d(TAG, "Directory created: $path")
            }
            created
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directory", e)
            false
        }
    }

    /**
     * Get folder size recursively
     */
    private fun getFolderSize(folder: File): Long {
        return try {
            var size = 0L
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isFile) {
                        file.length()
                    } else {
                        getFolderSize(file)
                    }
                }
            }
            size
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Search files by name
     */
    fun searchFiles(directory: String, query: String): List<FileInfo> {
        val results = mutableListOf<FileInfo>()
        searchFilesRecursive(File(directory), query, results)
        return results
    }

    /**
     * Recursive file search
     */
    private fun searchFilesRecursive(directory: File, query: String, results: MutableList<FileInfo>) {
        try {
            val files = directory.listFiles() ?: return
            
            for (file in files) {
                if (file.name.contains(query, ignoreCase = true)) {
                    results.add(
                        FileInfo(
                            name = file.name,
                            path = file.absolutePath,
                            size = if (file.isFile) file.length() else getFolderSize(file),
                            isDirectory = file.isDirectory,
                            lastModified = file.lastModified(),
                            canRead = file.canRead(),
                            canWrite = file.canWrite()
                        )
                    )
                }
                
                if (file.isDirectory) {
                    searchFilesRecursive(file, query, results)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching files", e)
        }
    }

    /**
     * Get common directories
     */
    fun getCommonDirectories(): Map<String, String> {
        return mapOf(
            "Downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
            "DCIM" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
            "Pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
            "Movies" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
            "Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath,
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath,
            "Internal" to Environment.getExternalStorageDirectory().absolutePath,
            "App" to context.filesDir.absolutePath
        )
    }

    /**
     * Get files as JSON
     */
    fun getFilesAsJson(path: String): JSONArray {
        val files = listFiles(path)
        val jsonArray = JSONArray()
        
        files.forEach { file ->
            jsonArray.put(file.toJson())
        }
        
        return jsonArray
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
    }

    /**
     * Data class for file info
     */
    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val isDirectory: Boolean,
        val lastModified: Long,
        val canRead: Boolean,
        val canWrite: Boolean
    ) {
        fun toJson(): JSONObject {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return JSONObject().apply {
                put("name", name)
                put("path", path)
                put("size", size)
                put("sizeFormatted", formatFileSize(size))
                put("isDirectory", isDirectory)
                put("lastModified", lastModified)
                put("lastModifiedFormatted", dateFormat.format(Date(lastModified)))
                put("canRead", canRead)
                put("canWrite", canWrite)
            }
        }
    }
}
