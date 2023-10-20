package model

import com.sdercolin.vlabeler.model.LogicalExpression
import com.sdercolin.vlabeler.model.LogicalNode
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class LogicalExpressionParsingTest {

    @Test
    fun testSingle() = match("#1", leaf(0))

    @Test
    fun test_Single() = match("(#1)", leaf(0))

    @Test
    fun testNot() = match("not #1", not(leaf(0)))

    @Test
    fun test_Not() = match("not (#1)", not(leaf(0)))

    @Test
    fun test__Not() = match("(not #1)", not(leaf(0)))

    @Test
    fun testAnd() = match("#1 and #2", and(leaf(0), leaf(1)))

    @Test
    fun test_And() = match("(#1 and #2)", and(leaf(0), leaf(1)))

    @Test
    fun testOr() = match("#1 or #2", or(leaf(0), leaf(1)))

    @Test
    fun testXor() = match("#1 xor #2", xor(leaf(0), leaf(1)))

    @Test
    fun testNotAnd() = match("not #1 and #2", and(not(leaf(0)), leaf(1)))

    @Test
    fun test_NotAnd() = match("(not #1) and #2", and(not(leaf(0)), leaf(1)))

    @Test
    fun testNot_And() = match("not (#1 and #2)", not(and(leaf(0), leaf(1))))

    @Test
    fun testAndNot() = match("#1 and not #2", and(leaf(0), not(leaf(1))))

    @Test
    fun testAnd_Not() = match("#1 and (not #2)", and(leaf(0), not(leaf(1))))

    @Test
    fun testAndAnd() = match(
        "#1 and #2 and #3",
        and(and(leaf(0), leaf(1)), leaf(2)),
    )

    @Test
    fun test_AndAnd() = match(
        "(#1 and #2) and #3",
        and(and(leaf(0), leaf(1)), leaf(2)),
    )

    @Test
    fun testAnd_And() = match(
        "#1 and (#2 and #3)",
        and(leaf(0), and(leaf(1), leaf(2))),
    )

    @Test
    fun testAndAnd_And() = match(
        "#1 and #2 and (#3 and #4)",
        and(and(leaf(0), leaf(1)), and(leaf(2), leaf(3))),
    )

    @Test
    fun testAnd_AndAnd() = match(
        "#1 and (#2 and #3) and #4",
        and(and(leaf(0), and(leaf(1), leaf(2))), leaf(3)),
    )

    @Test
    fun test_AndAnd_And() = match(
        "(#1 and #2) and #3 and #4",
        and(and(and(leaf(0), leaf(1)), leaf(2)), leaf(3)),
    )

    @Test
    fun testAnd__AndAnd() = match(
        "#1 and ((#2 and #3) and #4)",
        and(leaf(0), and(and(leaf(1), leaf(2)), leaf(3))),
    )

    @Test
    fun testAnd_And_And() = match(
        "#1 and (#2 and (#3 and #4))",
        and(leaf(0), and(leaf(1), and(leaf(2), leaf(3)))),
    )

    @Test
    fun testEmpty() = error("")

    @Test
    fun testUnknownCharacter() = error("a")

    @Test
    fun testMissingClosingBracket() = error("{1")

    @Test
    fun testMissingLeftOperand() = error("and #1")

    @Test
    fun testMissingLeftOperand2() = error("not and #1")

    @Test
    fun testMissingRightOperand() = error("#1 and")

    @Test
    fun testMissingLeftOperandOfNot() = error("not")

    @Test
    fun testUnmatchedParentheses() = error("(#1")

    @Test
    fun testUnmatchedParentheses2() = error("#1)")

    @Test
    fun testUnmatchedParentheses3() = error("(#1 and #2")

    @Test
    fun testEmptyParentheses() = error("#1 and ()")

    private fun match(expression: String, expected: LogicalNode) {
        val parsed = LogicalExpression.parse(expression).getOrThrow()
        assertEquals(expected, parsed.root)
    }

    private fun error(expression: String) {
        assertThrows<Throwable> {
            LogicalExpression.parse(expression).getOrThrow()
        }
    }

    private fun leaf(index: Int) = LogicalNode.Leaf(index)
    private fun and(left: LogicalNode, right: LogicalNode) = LogicalNode.And(left, right)
    private fun or(left: LogicalNode, right: LogicalNode) = LogicalNode.Or(left, right)
    private fun not(node: LogicalNode) = LogicalNode.Not(node)
    private fun xor(left: LogicalNode, right: LogicalNode) = LogicalNode.Xor(left, right)
}
