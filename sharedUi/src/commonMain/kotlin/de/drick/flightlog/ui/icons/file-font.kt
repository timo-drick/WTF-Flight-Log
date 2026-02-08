package de.drick.flightlog.ui.icons/*
The MIT License (MIT)

Copyright (c) 2019-2024 The Bootstrap Authors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BootstrapFileFont: ImageVector
    get() {
        if (_BootstrapFileFont != null) return _BootstrapFileFont!!
        
        _BootstrapFileFont = ImageVector.Builder(
            name = "file-font",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(10.943f, 4f)
                horizontalLineTo(5.057f)
                lineTo(5f, 6f)
                horizontalLineToRelative(0.5f)
                curveToRelative(0.18f, -1.096f, 0.356f, -1.192f, 1.694f, -1.235f)
                lineToRelative(0.293f, -0.01f)
                verticalLineToRelative(6.09f)
                curveToRelative(0f, 0.47f, -0.1f, 0.582f, -0.898f, 0.655f)
                verticalLineToRelative(0.5f)
                horizontalLineTo(9.41f)
                verticalLineToRelative(-0.5f)
                curveToRelative(-0.803f, -0.073f, -0.903f, -0.184f, -0.903f, -0.654f)
                verticalLineTo(4.755f)
                lineToRelative(0.298f, 0.01f)
                curveToRelative(1.338f, 0.043f, 1.514f, 0.14f, 1.694f, 1.235f)
                horizontalLineToRelative(0.5f)
                lineToRelative(-0.057f, -2f)
                close()
            }
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(4f, 0f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
                verticalLineToRelative(12f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                horizontalLineToRelative(8f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                verticalLineTo(2f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
                close()
                moveToRelative(0f, 1f)
                horizontalLineToRelative(8f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
                verticalLineToRelative(12f)
                arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
                horizontalLineTo(4f)
                arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
                verticalLineTo(2f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
            }
        }.build()
        
        return _BootstrapFileFont!!
    }

private var _BootstrapFileFont: ImageVector? = null

