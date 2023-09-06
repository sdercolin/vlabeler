import java.io.File
import java.util.Locale

fun findAllKotlinFiles(root: File): List<File> {
    val result = mutableListOf<File>()
    root.walkTopDown().forEach {
        if (it.isFile && it.extension == "kt") {
            result.add(it)
        }
    }
    return result
}

val allKotlinFiles = findAllKotlinFiles(File("").absoluteFile.parentFile.resolve("src"))

val constNames = mutableListOf<Triple<File, String?, String>>()

fun extractClasName(line: String): String? {
    if (line.contains("companion object")) return null
    listOf("class", "object").forEach keyword@{ keyword ->
        if (!line.contains(keyword)) return@keyword
        val match = Regex("$keyword ([A-Za-z]+)").find(line) ?: return@keyword
        return match.groupValues[1]
    }
    return null
}

fun saveConstNames(file: File) {
    val lines = file.readLines()
    val regex = Regex("const val ([A-Za-z]+) =")
    lines.forEachIndexed { index, line ->
        val constMatch = regex.find(line) ?: return@forEachIndexed
        var className: String? = null
        if (constMatch.range.first > 0) {
            val linesBefore = lines.subList(0, index).reversed()
            for (lineBefore in linesBefore) {
                className = extractClasName(lineBefore)
                if (className != null) {
                    break
                }
            }
        }
        constNames.add(Triple(file, className, constMatch.groupValues[1]))
    }
}

allKotlinFiles.forEach {
    saveConstNames(it)
}

val constNameMap = constNames.map { triple ->
    val (file, className, constName) = triple
    val newConstName = constName.replace(Regex("([A-Z]+)"), "_$1")
        .uppercase(Locale.getDefault()).trim('_')
    if (className != null) {
        Triple(file, "$className.$constName", "$className.$newConstName")
    } else {
        Triple(file, constName, newConstName)
    }
}

constNameMap.forEach { (file, old, new) ->
    println("${file.name}: $old -> $new")
}

allKotlinFiles.forEach { file ->
    val lines = file.readLines()
    val classNames = mutableListOf<String>()
    val newLines = lines.map { line ->
        val lineTrimmed = line.trim()
        // exclude comments
        if (lineTrimmed.startsWith("//") || lineTrimmed.startsWith("*")) {
            return@map line
        }
        extractClasName(line)?.let { classNames.add(it) }
        constNameMap.fold(line) { acc, (_, old, new) ->
            val oldName = old.split(".").lastOrNull()
            val newName = new.split(".").lastOrNull()
            if (listOf("Type", "Min", "Max").contains(oldName).not()) {
                if (listOf(
                        Regex("[(\\s\"\$\\.\\{]$oldName[)\\s,)\"\\.}]"),
                        Regex("[(\\s\"\$\\.\\{]$oldName$"),
                    ).none { it.find(acc) != null }
                ) return@fold acc
                acc.replace(oldName!!, newName!!)
            } else {
                if (listOf(
                        Regex("[(\\s\"\$\\{]$old[)\\s,)\"\\.]}"),
                        Regex("[(\\s\"\$\\{]$old$"),
                    ).none { it.find(acc) != null }
                ) return@fold acc
                acc.replace(old, new)
            }
        }
    }
    if (lines != newLines) {
        file.writeText(newLines.joinToString("") { "$it\n" })
    }
}
