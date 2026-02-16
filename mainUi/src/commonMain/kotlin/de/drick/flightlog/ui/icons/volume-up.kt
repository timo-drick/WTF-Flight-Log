/*
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

val IconVolumeUp: ImageVector
    get() {
        if (_BootstrapVolumeUp != null) return _BootstrapVolumeUp!!
        
        _BootstrapVolumeUp = ImageVector.Builder(
            name = "volume-up",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(11.536f, 14.01f)
                arcTo(8.47f, 8.47f, 0f, false, false, 14.026f, 8f)
                arcToRelative(8.47f, 8.47f, 0f, false, false, -2.49f, -6.01f)
                lineToRelative(-0.708f, 0.707f)
                arcTo(7.48f, 7.48f, 0f, false, true, 13.025f, 8f)
                curveToRelative(0f, 2.071f, -0.84f, 3.946f, -2.197f, 5.303f)
                close()
            }
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(10.121f, 12.596f)
                arcTo(6.48f, 6.48f, 0f, false, false, 12.025f, 8f)
                arcToRelative(6.48f, 6.48f, 0f, false, false, -1.904f, -4.596f)
                lineToRelative(-0.707f, 0.707f)
                arcTo(5.48f, 5.48f, 0f, false, true, 11.025f, 8f)
                arcToRelative(5.48f, 5.48f, 0f, false, true, -1.61f, 3.89f)
                close()
            }
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(10.025f, 8f)
                arcToRelative(4.5f, 4.5f, 0f, false, true, -1.318f, 3.182f)
                lineTo(8f, 10.475f)
                arcTo(3.5f, 3.5f, 0f, false, false, 9.025f, 8f)
                curveToRelative(0f, -0.966f, -0.392f, -1.841f, -1.025f, -2.475f)
                lineToRelative(0.707f, -0.707f)
                arcTo(4.5f, 4.5f, 0f, false, true, 10.025f, 8f)
                moveTo(7f, 4f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, -0.812f, -0.39f)
                lineTo(3.825f, 5.5f)
                horizontalLineTo(1.5f)
                arcTo(0.5f, 0.5f, 0f, false, false, 1f, 6f)
                verticalLineToRelative(4f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, 0.5f, 0.5f)
                horizontalLineToRelative(2.325f)
                lineToRelative(2.363f, 1.89f)
                arcTo(0.5f, 0.5f, 0f, false, false, 7f, 12f)
                close()
                moveTo(4.312f, 6.39f)
                lineTo(6f, 5.04f)
                verticalLineToRelative(5.92f)
                lineTo(4.312f, 9.61f)
                arcTo(0.5f, 0.5f, 0f, false, false, 4f, 9.5f)
                horizontalLineTo(2f)
                verticalLineToRelative(-3f)
                horizontalLineToRelative(2f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, 0.312f, -0.11f)
            }
        }.build()
        
        return _BootstrapVolumeUp!!
    }

private var _BootstrapVolumeUp: ImageVector? = null

