package dev.zwander.cameraxinfo.util

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.safetynet.SafetyNet
import kotlinx.coroutines.tasks.await
import java.util.UUID

private var cachedResult: Boolean? = null

suspend fun Context.verifySafetyNet(): Boolean {
    return cachedResult ?: run {
        val key = "AIzaSyCmC5_n3Nu60HUiebUfzyEC_aRW9VPLjII"
        val nonce = UUID.randomUUID().toString()

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            return false
        }

        (SafetyNet.getClient(this).attest(nonce.toByteArray(), key).await().jwsResult != null).also {
            cachedResult = it
        }
    }
}
