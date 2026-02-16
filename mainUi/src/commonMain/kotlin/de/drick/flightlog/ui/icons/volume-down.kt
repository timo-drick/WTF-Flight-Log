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

val IconVolumeDown: ImageVector
    get() {
        if (_BootstrapVolumeDown != null) return _BootstrapVolumeDown!!
        
        _BootstrapVolumeDown = ImageVector.Builder(
            name = "volume-down",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(9f, 4f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, -0.812f, -0.39f)
                lineTo(5.825f, 5.5f)
                horizontalLineTo(3.5f)
                arcTo(0.5f, 0.5f, 0f, false, false, 3f, 6f)
                verticalLineToRelative(4f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, 0.5f, 0.5f)
                horizontalLineToRelative(2.325f)
                lineToRelative(2.363f, 1.89f)
                arcTo(0.5f, 0.5f, 0f, false, false, 9f, 12f)
                close()
                moveTo(6.312f, 6.39f)
                lineTo(8f, 5.04f)
                verticalLineToRelative(5.92f)
                lineTo(6.312f, 9.61f)
                arcTo(0.5f, 0.5f, 0f, false, false, 6f, 9.5f)
                horizontalLineTo(4f)
                verticalLineToRelative(-3f)
                horizontalLineToRelative(2f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, 0.312f, -0.11f)
                moveTo(12.025f, 8f)
                arcToRelative(4.5f, 4.5f, 0f, false, true, -1.318f, 3.182f)
                lineTo(10f, 10.475f)
                arcTo(3.5f, 3.5f, 0f, false, false, 11.025f, 8f)
                arcTo(3.5f, 3.5f, 0f, false, false, 10f, 5.525f)
                lineToRelative(0.707f, -0.707f)
                arcTo(4.5f, 4.5f, 0f, false, true, 12.025f, 8f)
            }
        }.build()
        
        return _BootstrapVolumeDown!!
    }

private var _BootstrapVolumeDown: ImageVector? = null

