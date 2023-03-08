package dev.zwander.cameraxinfo.util

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.zwander.cameraxinfo.util.ResultSaver.save

val uploadResultGson: Gson = GsonBuilder().registerTypeAdapterFactory(
    RuntimeTypeAdapterFactory.of(UploadResult::class.java)
        .registerSubtype(UploadResult.ErrorResult::class.java)
        .registerSubtype(UploadResult.Uploading::class.java)
        .registerSubtype(UploadResult.UploadFailure::class.java)
        .registerSubtype(UploadResult.DuplicateData::class.java)
        .registerSubtype(UploadResult.SafetyNetFailure::class.java)
        .registerSubtype(UploadResult.SignInFailure::class.java)
        .registerSubtype(UploadResult.Success::class.java)
        .recognizeSubtypes()
).create()

object ResultSaver : Saver<MutableState<UploadResult?>, String> {
    override fun restore(value: String): MutableState<UploadResult?> {
        return mutableStateOf(
            value.let {
                uploadResultGson.fromJson(
                    it,
                    object : TypeToken<UploadResult>() {}.type
                )
            }
        )
    }

    override fun SaverScope.save(value: MutableState<UploadResult?>): String {
        return value.value?.let { uploadResultGson.toJson(it) } ?: ""
    }
}

object ErrorSaver : Saver<MutableState<Exception?>, String> {
    override fun restore(value: String): MutableState<Exception?> {
        return mutableStateOf(
            uploadResultGson.fromJson(value, object : TypeToken<Exception>() {}.type)
        )
    }

    override fun SaverScope.save(value: MutableState<Exception?>): String {
        return value.value?.let { uploadResultGson.toJson(it) } ?: ""
    }
}

object UploadErrorSaver : Saver<MutableState<Pair<Exception?, Uri?>?>, String> {
    private const val DELIM = ",,,,,,,,,,,,,,"

    override fun restore(value: String): MutableState<Pair<Exception?, Uri?>?> {
        val (error, uri) = value.split(DELIM)

        if (error.isBlank() || uri.isBlank()) {
            return mutableStateOf(null)
        }

        return mutableStateOf(
            ErrorSaver.restore(error).value to Uri.parse(uri)
        )
    }

    override fun SaverScope.save(value: MutableState<Pair<Exception?, Uri?>?>): String {
        return with (ErrorSaver) {
            "${save(mutableStateOf(value.value?.first))}$DELIM${value.value?.second?.toString() ?: ""}"
        }
    }
}
