package com.andrecoura.homemonitor.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Two Material icons missing from material-icons-core, drawn from their path data
 * so the app does not pull the extended icon set (several MB of dex) for two glyphs.
 */
object AppIcons {
    val Videocam =
        icon(
            "Videocam",
            "M17,10.5V7c0,-0.55 -0.45,-1 -1,-1H4c-0.55,0 -1,0.45 -1,1v10c0,0.55 0.45,1 1,1h12c0.55,0 1,-0.45 1,-1v-3.5l4,4v-11l-4,4z",
        )
    val Speaker =
        icon(
            "Speaker",
            "M17,2H7c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,1.99 2,1.99L17,22c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM12,4c1.1,0 2,0.9 2,2s-0.9,2 -2,2 -2,-0.9 -2,-2 0.9,-2 2,-2zM12,20c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
        )

    private fun icon(
        name: String,
        path: String,
    ): ImageVector =
        ImageVector
            .Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()
}
