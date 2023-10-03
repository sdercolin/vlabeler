package com.sdercolin.vlabeler.repository

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.util.AppDir
import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeCollection
import org.apache.fontbox.ttf.TrueTypeFont
import java.io.File

object FontRepository {

    sealed class FontOption(val name: String, val fontFamily: FontFamily) {
        sealed class BuiltIn(name: String, fontFamily: FontFamily) : FontOption(name, fontFamily) {
            object Default : BuiltIn("Default", FontFamily.Default)
            object SansSerif : BuiltIn("SansSerif", FontFamily.SansSerif)
            object Serif : BuiltIn("Serif", FontFamily.Serif)
            object Monospace : BuiltIn("Monospace", FontFamily.Monospace)
            object Cursive : BuiltIn("Cursive", FontFamily.Cursive)
        }

        class Custom(name: String, fontFamily: FontFamily) : FontOption(name, fontFamily)
    }

    private val SUPPORTED_FONT_EXTENSIONS = listOf("ttf", "otf", "ttc")
    private const val DIRECTORY_NAME = "fonts"
    val fontDirectory: File = AppDir.resolve(DIRECTORY_NAME)
    private val fontFiles
        get() = fontDirectory.listFiles().orEmpty()
            .filter { it.extension in SUPPORTED_FONT_EXTENSIONS }

    private fun TrueTypeFont.parse(): Pair<String, Font> {
        val familyName = naming.fontFamily
        val subName = naming.fontSubFamily
        val weight = oS2Windows.weightClass
        val isItalic = subName.contains("italic", ignoreCase = true)
        println("Loading font $familyName $subName: Weight=$weight, isItalic=$isItalic")
        return familyName to Font(
            identity = "$familyName $subName",
            data = originalData.readAllBytes(),
            weight = FontWeight(weight),
            style = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        )
    }

    private fun loadCustomFontFamilies(): List<Pair<String, FontFamily>> {
        val fonts = mutableListOf<Pair<String, Font>>()
        for (fontFile in fontFiles) {
            when (fontFile.extension) {
                "ttc" -> {
                    val ttc = TrueTypeCollection(fontFile)
                    var index = 0
                    ttc.processAllFonts { font ->
                        if (index > 0) {
                            // we cannot load multiple fonts from a single ttc file for now
                            return@processAllFonts
                        }
                        fonts.add(font.parse())
                        index++
                    }
                }
                "ttf" -> {
                    val parser = TTFParser()
                    val font = parser.parse(fontFile)
                    fonts.add(font.parse())
                }
                "otf" -> {
                    val parser = OTFParser()
                    val font = parser.parse(fontFile)
                    fonts.add(font.parse())
                }
                else -> {
                    // ignore
                }
            }
        }
        return fonts.groupBy { it.first }
            .map { (name, fonts) ->
                name to FontFamily(fonts.map { it.second })
            }
    }

    private var loadedFonts: List<FontOption>? = null

    fun initialize() {
        fontDirectory.apply {
            if (exists().not()) {
                mkdirs()
            }
        }
        load()
    }

    fun load() {
        val builtIn = listOf(
            FontOption.BuiltIn.Default,
            FontOption.BuiltIn.SansSerif,
            FontOption.BuiltIn.Serif,
            FontOption.BuiltIn.Monospace,
            FontOption.BuiltIn.Cursive,
        )
        val custom = loadCustomFontFamilies().map { (name, fontFamily) ->
            FontOption.Custom(name, fontFamily)
        }
        loadedFonts = (builtIn + custom).distinctBy { it.name }
    }

    fun listAllNames() = loadedFonts.orEmpty().map { it.name }

    private fun get(name: String) = loadedFonts?.firstOrNull {
        it.name == name
    }

    fun hasFontFamily(name: String) = get(name) != null

    fun getFontFamily(name: String): FontFamily {
        val found = get(name)
        if (found == null) {
            Log.error("Cannot find font family $name.")
            return FontFamily.Default
        }
        return found.fontFamily
    }
}
