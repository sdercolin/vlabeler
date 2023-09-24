package com.sdercolin.vlabeler.ui.string

/**
 * A pair of the tag included in a clickable string and the corresponding action.
 */
class ClickableTag(
    val tag: String,
    val onClick: () -> Unit,
)

/**
 * A string with clickable parts.
 */
class ClickableString(
    val text: String,
    val clickables: List<Pair<String, () -> Unit>>,
)

/**
 * Extract clickable tags from a string. For example, given a text "text1@click1{text2}text3", the result should be
 * "text1text2text3" and a list of ["click1" to onClick1].
 */
fun extractClickables(text: String, tags: List<ClickableTag>): ClickableString {
    val pattern = Regex("""@(\w+)\{([^}]+)}""")
    val result = pattern.findAll(text)
    val restoredText = result.fold(text) { acc, matchResult ->
        val tag = matchResult.groupValues[1]
        val clickable = tags.find { it.tag == tag }
        if (clickable != null) {
            acc.replace(matchResult.value, matchResult.groupValues[2])
        } else {
            acc
        }
    }
    return ClickableString(
        text = restoredText,
        clickables = result.mapNotNull { matchResult ->
            val tag = matchResult.groupValues[1]
            val clickable = tags.find { it.tag == tag }
            if (clickable != null) {
                matchResult.groupValues[2] to clickable.onClick
            } else {
                null
            }
        }.toList(),
    )
}
