package com.sdercolin.vlabeler.model

/**
 * Model for a logical expression used in an [EntrySelector].
 */
data class LogicalExpression(val root: LogicalNode, val requiredPlaceholderCount: Int) {

    private constructor(root: LogicalNode) : this(root, root.getMaxPlaceholderIndex() + 1)

    fun evaluate(placeholderValues: List<Boolean>): Boolean = root.evaluate(placeholderValues)

    companion object {
        fun parse(expression: String): Result<LogicalExpression> =
            runCatching { LogicalExpression(parseNode(expression).toNode()) }

        fun default(count: Int): LogicalExpression? = (0 until count).fold<Int, LogicalNode?>(
            null,
        ) { acc, index ->
            if (acc == null) LogicalNode.Leaf(index) else LogicalNode.And(acc, LogicalNode.Leaf(index))
        }?.let { LogicalExpression(it, count) }
    }
}

sealed class LogicalNode {

    fun evaluate(placeholderValues: List<Boolean>): Boolean = when (this) {
        is And -> left.evaluate(placeholderValues) && right.evaluate(placeholderValues)
        is Or -> left.evaluate(placeholderValues) || right.evaluate(placeholderValues)
        is Not -> !node.evaluate(placeholderValues)
        is Xor -> left.evaluate(placeholderValues) xor right.evaluate(placeholderValues)
        is Leaf -> placeholderValues[placeholderIndex]
    }

    fun getMaxPlaceholderIndex(): Int = when (this) {
        is And -> maxOf(left.getMaxPlaceholderIndex(), right.getMaxPlaceholderIndex())
        is Or -> maxOf(left.getMaxPlaceholderIndex(), right.getMaxPlaceholderIndex())
        is Not -> node.getMaxPlaceholderIndex()
        is Xor -> maxOf(left.getMaxPlaceholderIndex(), right.getMaxPlaceholderIndex())
        is Leaf -> placeholderIndex
    }

    data class And(val left: LogicalNode, val right: LogicalNode) : LogicalNode()
    data class Or(val left: LogicalNode, val right: LogicalNode) : LogicalNode()
    data class Not(val node: LogicalNode) : LogicalNode()
    data class Xor(val left: LogicalNode, val right: LogicalNode) : LogicalNode()
    data class Leaf(val placeholderIndex: Int) : LogicalNode()
}

private class NestedRawExpression(
    val text: String,
    val children: List<String>,
)

private fun parseNode(expression: String): RawNode {
    val nested = buildNested(expression.cleanup())
    return parseNested(nested)
}

private fun String.cleanup() = replace(Regex("""\s"""), "")

private fun buildNested(expression: String): NestedRawExpression {
    var depth = 0
    val children = mutableListOf<String>()
    var childIndex = 0
    val result = StringBuilder()
    var currentChild = StringBuilder()
    for (i in expression.indices) {
        val char = expression[i]
        when (char) {
            '(' -> {
                if (depth == 0) {
                    require(currentChild.isEmpty())
                    depth++
                    continue
                }
                depth++
            }
            ')' -> {
                depth--
                if (depth == 0) {
                    require(currentChild.isNotEmpty()) { "Empty parenthesis" }
                    children.add(currentChild.toString())
                    currentChild = StringBuilder()
                    childIndex++
                    result.append("{$childIndex}")
                    continue
                }
            }
        }
        if (depth > 0) {
            currentChild.append(char)
        } else {
            result.append(char)
        }
    }
    require(depth == 0) { "Unmatched parenthesis" }
    return NestedRawExpression(result.toString(), children)
}

private fun parseTokens(expression: String): List<LogicalToken> {
    val tokens = mutableListOf<LogicalToken>()
    var remaining = expression
    while (remaining.isNotEmpty()) {
        val (token, length) = extractFirstToken(remaining, 0)
        tokens.add(token)
        remaining = remaining.drop(length)
    }
    return tokens
}

private fun parseNested(expression: NestedRawExpression): RawNode {
    val tokens = parseTokens(expression.text)
    var root = RawNode(null)
    var current = root
    var finished = false

    fun pop() {
        val parent = current.parent
        if (parent == null) {
            finished = true
        } else {
            when {
                parent.isPendingLeft -> {
                    parent.left = current
                }
                parent.isPendingRight -> {
                    parent.right = current
                }
            }
            current = parent
        }
        if (current.isComplete && !finished) pop()
    }

    fun error(token: LogicalToken) {
        throw IllegalArgumentException("Unexpected token: $token")
    }

    for (token in tokens) {
        if (finished) {
            finished = false
            val newRoot = RawNode(null, left = root)
            root.parent = newRoot
            root = newRoot
            current = root
        }
        when (token) {
            is LogicalToken.Placeholder,
            is LogicalToken.NestedPlacedolder,
            -> {
                val rawNode = when (token) {
                    is LogicalToken.Placeholder -> RawNode(current, leaf = token)
                    is LogicalToken.NestedPlacedolder -> parseNode(expression.children[token.index])
                    else -> throw IllegalStateException()
                }
                when {
                    current.left == null && current.operator == null -> {
                        current.left = rawNode
                    }
                    current.operator != null && current.right == null -> {
                        current.right = rawNode
                        pop()
                    }
                    else -> error(token)
                }
            }
            is LogicalToken.And,
            is LogicalToken.Or,
            is LogicalToken.Xor,
            -> {
                when {
                    current.left != null && current.operator == null -> {
                        current.operator = token
                    }
                    else -> error(token)
                }
            }
            is LogicalToken.Not -> {
                when {
                    current.left == null && current.operator == null -> {
                        current.operator = token
                    }
                    current.left != null && current.operator != null && current.right == null -> {
                        val newNode = RawNode(current, operator = token)
                        current.right = newNode
                        current = newNode
                    }
                    else -> error(token)
                }
            }
        }
    }
    require(current == root) { "Unmatched layers" }
    return root
}

private class RawNode(
    var parent: RawNode?,
    var left: RawNode? = null,
    var operator: LogicalToken? = null,
    var right: RawNode? = null,
    var leaf: LogicalToken.Placeholder? = null,
) {
    val isComplete: Boolean
        get() = when {
            leaf != null -> true
            operator == null -> false
            operator is LogicalToken.Not -> right != null
            else -> left != null && right != null
        }

    val isPendingLeft: Boolean
        get() = when {
            leaf != null -> false
            operator is LogicalToken.Not -> false
            operator == null && left == null -> true
            else -> false
        }

    val isPendingRight: Boolean
        get() = when {
            leaf != null -> false
            operator is LogicalToken.Not -> right == null
            operator != null && left != null -> true
            else -> false
        }

    fun toNode(): LogicalNode {
        val left = left
        val operator = operator
        val right = right
        val leaf = leaf
        when {
            leaf != null -> {
                require(left == null && operator == null && right == null) { "Invalid leaf node" }
                return LogicalNode.Leaf(leaf.index)
            }
            operator == null -> {
                if (left != null && right == null) {
                    return left.toNode()
                }
                error("Invalid node: no operator")
            }
            left == null && right != null && operator == LogicalToken.Not -> {
                return LogicalNode.Not(right.toNode())
            }
            left != null && right != null -> {
                return when (operator) {
                    LogicalToken.And -> LogicalNode.And(left.toNode(), right.toNode())
                    LogicalToken.Or -> LogicalNode.Or(left.toNode(), right.toNode())
                    LogicalToken.Xor -> LogicalNode.Xor(left.toNode(), right.toNode())
                    else -> error("Invalid node: invalid operator")
                }
            }
            else -> {
                error("Invalid node: invalid structure")
            }
        }
    }
}

private sealed class LogicalToken {
    object And : LogicalToken()
    object Or : LogicalToken()
    object Not : LogicalToken()
    object Xor : LogicalToken()
    data class NestedPlacedolder(val index: Int) : LogicalToken()
    data class Placeholder(val index: Int) : LogicalToken()
}

private val tokenMap = mapOf(
    "&&" to LogicalToken.And,
    "and" to LogicalToken.And,
    "AND" to LogicalToken.And,
    "||" to LogicalToken.Or,
    "or" to LogicalToken.Or,
    "OR" to LogicalToken.Or,
    "!" to LogicalToken.Not,
    "not" to LogicalToken.Not,
    "NOT" to LogicalToken.Not,
    "^^" to LogicalToken.Xor,
    "xor" to LogicalToken.Xor,
    "XOR" to LogicalToken.Xor,
)

private fun extractFirstToken(expression: String, index: Int): Pair<LogicalToken, Int> {
    val placeholderMatch = Regex("""^#\d+""").find(expression, index)
    if (placeholderMatch != null) {
        val placeholder = placeholderMatch.value
        val placeholderIndex = placeholder.drop(1).toIntOrNull()?.minus(1)
            ?: throw IllegalArgumentException("Invalid placeholder (not int): $placeholder")
        if (placeholderIndex < 0) throw IllegalArgumentException("Invalid placeholder (negative): $placeholder")
        return LogicalToken.Placeholder(placeholderIndex) to placeholder.length
    }
    val nestedPlaceholderMatch = Regex("""^\{\d+}""").find(expression, index)
    if (nestedPlaceholderMatch != null) {
        val nestedPlaceholder = nestedPlaceholderMatch.value
        val nestedPlaceholderIndex = nestedPlaceholder.drop(1).dropLast(1).toIntOrNull()?.minus(1)
            ?: throw IllegalArgumentException("Invalid placeholder (not int): $nestedPlaceholder")
        if (nestedPlaceholderIndex < 0) {
            throw IllegalArgumentException("Invalid placeholder (negative): $nestedPlaceholderIndex")
        }
        return LogicalToken.NestedPlacedolder(nestedPlaceholderIndex) to nestedPlaceholder.length
    }
    return tokenMap.entries
        .firstOrNull { expression.startsWith(it.key, index) }
        ?.let { it.value to it.key.length }
        ?: throw IllegalArgumentException("Unknown token: ${expression[index]}")
}
