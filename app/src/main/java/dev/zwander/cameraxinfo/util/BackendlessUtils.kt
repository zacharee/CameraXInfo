package dev.zwander.cameraxinfo.util

import com.android.internal.R.attr.path
import com.backendless.Backendless
import com.backendless.files.FileInfo
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

object BackendlessUtils {
    fun setup() {
        Backendless.initApp("86A52F75-23E8-C95A-FF52-3F61E7086D00", "12271E7A-E4E4-4674-9038-728E1C625FEF")
    }

    fun ensureLogin(): Boolean {
        return if (Backendless.UserService.CurrentUser() == null) {
            Backendless.UserService.loginAsGuest(true) != null
        } else {
            true
        }
    }

    fun uploadFile(path: String, name: String, content: String): Boolean {
        return Backendless.Files.uploadFromStream(
            {
                content.byteInputStream().use { input -> input.copyTo(it) }
            },
            name,
            path
        ) != null
    }

    fun listDirectory(path: String): List<FileInfo> {
        val list = mutableListOf<FileInfo>()
        val fileCount = Backendless.Files.getFileCount(path, "*", false, true)

        for (i in 0 until fileCount step 100) {
            list.addAll(Backendless.Files.listing(path, "*", false, 100, i))
        }

        return list
    }

    fun getAllFiles(): List<FileInfo> {
        val list = mutableListOf<FileInfo>()
        val fileCount = Backendless.Files.getFileCount("/CameraData", "*.json", false, true)

        for (i in 0 until fileCount step 100) {
            list.addAll(Backendless.Files.listing("/CameraData", "*.json", false, 100, i))
        }

        return list
    }

    suspend fun getContent(file: FileInfo): String {
        return HttpClient(Android).get(file.publicUrl).bodyAsText()
    }
}