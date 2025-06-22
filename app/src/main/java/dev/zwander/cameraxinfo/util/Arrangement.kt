package dev.zwander.cameraxinfo.util

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp

object ArrangementExt {
    @SuppressLint("ComposableNaming")
    @Composable
    fun SpaceEvenly(spacing: Dp): Arrangement.HorizontalOrVertical {
        return remember(spacing) {
            object : Arrangement.HorizontalOrVertical by Arrangement.SpaceEvenly {
                override val spacing: Dp = spacing
            }
        }
    }
}
