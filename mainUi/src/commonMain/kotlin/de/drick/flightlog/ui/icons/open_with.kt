package de.drick.flightlog.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MaterialIconsOpenWith: ImageVector
    get() {
        if (_MaterialIconsOpenWith != null) return _MaterialIconsOpenWith!!
        
        _MaterialIconsOpenWith = ImageVector.Builder(
            name = "open_with",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(10f, 9f)
                horizontalLineToRelative(4f)
                verticalLineTo(6f)
                horizontalLineToRelative(3f)
                lineToRelative(-5f, -5f)
                lineToRelative(-5f, 5f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(3f)
                close()
                moveTo(9f, 10f)
                horizontalLineTo(6f)
                verticalLineTo(7f)
                lineToRelative(-5f, 5f)
                lineToRelative(5f, 5f)
                verticalLineToRelative(-3f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(-4f)
                close()
                moveTo(14f, 15f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(3f)
                horizontalLineTo(7f)
                lineToRelative(5f, 5f)
                lineToRelative(5f, -5f)
                horizontalLineToRelative(-3f)
                verticalLineToRelative(-3f)
                close()
                moveTo(15f, 14f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(3f)
                lineToRelative(5f, -5f)
                lineToRelative(-5f, -5f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(-3f)
                verticalLineToRelative(4f)
                close()
            }
        }.build()
        
        return _MaterialIconsOpenWith!!
    }

private var _MaterialIconsOpenWith: ImageVector? = null
