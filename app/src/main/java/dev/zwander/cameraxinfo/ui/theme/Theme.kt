package dev.zwander.cameraxinfo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import dev.zwander.cameraxinfo.R

@Composable
fun CameraXInfoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = colorResource(id = R.color.colorPrimary),
            secondary = colorResource(id = R.color.colorSecondary),
        )
        else -> lightColorScheme(
            primary = colorResource(id = R.color.colorPrimary),
            secondary = colorResource(id = R.color.colorSecondary)
        )
    }.run {
        copy(
            outlineVariant = this.contentColorFor(this.surface)
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.apply {
                statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(this, view).isAppearanceLightStatusBars = darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        androidx.compose.material.MaterialTheme(
            colors = if (darkTheme) darkColors(
                primary = colorScheme.primary,
                primaryVariant = colorScheme.primaryContainer,
                onPrimary = colorScheme.onPrimary,
                secondary = colorScheme.secondary,
                secondaryVariant = colorScheme.secondaryContainer,
                onSecondary = colorScheme.onSecondary,
                background = colorScheme.background,
                onBackground = colorScheme.onBackground,
                surface = colorScheme.surface,
                onSurface = colorScheme.onSurface,
                error = colorScheme.error,
                onError = colorScheme.onError
            ) else lightColors(
                primary = colorScheme.primary,
                primaryVariant = colorScheme.primaryContainer,
                onPrimary = colorScheme.onPrimary,
                secondary = colorScheme.secondary,
                secondaryVariant = colorScheme.secondaryContainer,
                onSecondary = colorScheme.onSecondary,
                background = colorScheme.background,
                onBackground = colorScheme.onBackground,
                surface = colorScheme.surface,
                onSurface = colorScheme.onSurface,
                error = colorScheme.error,
                onError = colorScheme.onError
            )
        ) {
            content()
        }
    }
}