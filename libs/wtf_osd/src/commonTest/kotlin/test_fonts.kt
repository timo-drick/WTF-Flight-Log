import de.drick.wtf_osd.FontVariant
import de.drick.wtf_osd.loadOsdFont
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class FontTest {

    @Test
    fun testLoadAllFonts() = runTest {
        FontVariant.entries.forEach { fontVariant ->
            val font = loadOsdFont(FontVariant.BETAFLIGHT)
            assertNotNull(font)
        }
    }
}