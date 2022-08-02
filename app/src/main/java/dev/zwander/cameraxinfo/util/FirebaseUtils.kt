package dev.zwander.cameraxinfo.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await

suspend fun <T> Task<T>.awaitCatchingError(): T {
    addOnFailureListener {  }

    return await()
}