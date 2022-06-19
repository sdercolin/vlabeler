package com.sdercolin.vlabeler.util

fun String.matchGroups(regex: Regex): List<String> {
    return regex.find(this)?.groupValues.orEmpty().drop(1)
}

fun String.replaceWithVariables(variables: Map<String, Any>): String {
    val matches = Regex("""(?<=\{)[^{}]*(?=})""").findAll(this)
    var result = this
    for (match in matches) {
        result = result.replace("{${match.value}}", variables[match.value].toString())
    }
    return result
}
