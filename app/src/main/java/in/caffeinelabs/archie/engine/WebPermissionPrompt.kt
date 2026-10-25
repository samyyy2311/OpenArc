package `in`.caffeinelabs.archie.engine

import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest

sealed interface WebPermissionPrompt {
    val origin: String

    data class Media(
        override val origin: String,
        val request: PermissionRequest,
        val humanReadableLabels: List<String>,
        val requiredAndroidPermissions: List<String>
    ) : WebPermissionPrompt

    data class Geolocation(
        override val origin: String,
        val callback: GeolocationPermissions.Callback
    ) : WebPermissionPrompt
}
