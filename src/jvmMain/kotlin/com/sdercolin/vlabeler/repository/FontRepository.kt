package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.serialization.Serializable
import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeCollection
import org.apache.fontbox.ttf.TrueTypeFont
import java.io.File

object FontRepository {

    /**
     * A definition of a font family.
     *
     * @property name The name of the font family.
     * @property fonts The fonts in this family.
     */
    @Serializable
    @Immutable
    data class FontFamilyDefinition(
        val name: String,
        val fonts: List<FontDefinition>,
    ) {

        /**
         * A definition of a font.
         *
         * @property path The path to the font file. It can be an absolute path or a relative path to the
         *     [fontDirectory].
         * @property weight The weight of the font. If not specified, the weight will be determined by the metadata of
         *     the font file.
         * @property isItalic Whether the font is italic. If not specified, the value will be determined by the metadata
         *     of the font file.
         */
        @Serializable
        @Immutable
        data class FontDefinition(
            val path: String,
            val weight: Int? = null,
            val isItalic: Boolean? = null,
        )
    }

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
    private const val FONT_DEF_EXTENSION = "font.json"
    private const val DIRECTORY_NAME = "fonts"
    private const val README_FILE_NAME = "readme.txt"
    val fontDirectory: File = AppDir.resolve(DIRECTORY_NAME)
    private val fontFamilyDefs: List<FontFamilyDefinition>
        get() = fontDirectory.listFiles().orEmpty()
            .filter { it.name.endsWith(FONT_DEF_EXTENSION) }
            .mapNotNull { defFile ->
                runCatching { defFile.readText().parseJson<FontFamilyDefinition>() }.getOrElse {
                    Log.error(it)
                    Log.error("Cannot read font definition file ${defFile.absolutePath}.")
                    null
                }
            }
    private val fontFiles
        get() = fontDirectory.listFiles().orEmpty()
            .filter { it.extension in SUPPORTED_FONT_EXTENSIONS }

    private fun TrueTypeFont.parse(definition: FontFamilyDefinition.FontDefinition?): Pair<String, Font> {
        val familyName = naming.fontFamily
        val subName = naming.fontSubFamily
        val weight = definition?.weight ?: oS2Windows.weightClass
        val isItalic = definition?.isItalic ?: subName.contains("italic", ignoreCase = true)
        Log.info("Loading font $familyName $subName: Weight=$weight, isItalic=$isItalic")
        return familyName to Font(
            identity = "$familyName $subName",
            data = originalData.readAllBytes(),
            weight = FontWeight(weight),
            style = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        )
    }

    private fun parseFontFile(file: File, definition: FontFamilyDefinition.FontDefinition?): Pair<String, Font>? {
        val results = mutableListOf<Pair<String, Font>>()
        when (file.extension) {
            "ttc" -> {
                val ttc = TrueTypeCollection(file)
                var index = 0
                ttc.processAllFonts { font ->
                    if (index > 0) {
                        // we cannot load multiple fonts from a single ttc file for now
                        return@processAllFonts
                    }
                    results.add(font.parse(definition))
                    index++
                }
            }
            "ttf" -> {
                val parser = TTFParser()
                val font = parser.parse(file)
                results.add(font.parse(definition))
            }
            "otf" -> {
                val parser = OTFParser()
                val font = parser.parse(file)
                results.add(font.parse(definition))
            }
            else -> {
                // ignore
            }
        }
        return results.firstOrNull()
    }

    private fun loadCustomFontFamilies(): List<Pair<String, FontFamily>> {
        val result = mutableListOf<Pair<String, FontFamily>>()

        // load fonts with definition files
        val usedFontFiles = mutableListOf<String>()
        fontFamilyDefs.forEach { fontFamilyDef ->
            val fonts = fontFamilyDef.fonts.mapNotNull { fontDef ->
                val fontFile = fontDirectory.resolve(fontDef.path)
                usedFontFiles.add(fontFile.absolutePath)
                parseFontFile(fontFile, fontDef)
            }
            if (fonts.isEmpty()) return@forEach
            result.add(fontFamilyDef.name to FontFamily(fonts.map { it.second }))
        }

        // load fonts without definition files
        val fonts = mutableListOf<Pair<String, Font>>()
        fontFiles.filterNot { it.absolutePath in usedFontFiles }.forEach { fontFile ->
            parseFontFile(fontFile, definition = null)?.let { fonts.add(it) }
        }
        result.addAll(
            fonts.groupBy { it.first }
                .map { (name, fonts) ->
                    name to FontFamily(fonts.map { it.second })
                },
        )
        return result.distinctBy { it.first }.sortedBy { it.first }
    }

    private var loadedFonts: List<FontOption>? = null

    fun initialize(appRecord: AppRecord) {
        fontDirectory.apply {
            if (exists().not()) {
                mkdirs()
            }
        }
        val readmeFile = fontDirectory.resolve(README_FILE_NAME)
        if (appRecord.appVersionLastLaunched < appVersion || readmeFile.exists().not()) {
            useResource(Resources.customFontReadme) {
                val readme = it.bufferedReader().readText()
                readmeFile.writeText(readme)
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
