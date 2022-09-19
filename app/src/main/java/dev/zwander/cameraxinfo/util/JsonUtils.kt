package dev.zwander.cameraxinfo.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

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