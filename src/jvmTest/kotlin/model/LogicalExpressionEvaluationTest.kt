package model

import com.sdercolin.vlabeler.model.LogicalNode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LogicalExpressionEvaluationTest {

    private val placeholderValues = listOf(true, false, true, false)

    @Test
    fun testLeaf() = match(leaf(0), true)

    @Test
    fun testNot() = match(not(0), false)

    @Test
    fun testAnd() = match(and(0, 1), false)

    @Test
    fun testAnd2() = match(and(0, 2), true)

    @Test
    fun testOr() = match(or(0, 1), true)

    @Test
    fun testOr2() = match(or(1, 3), false)

    @Test
    fun testXor() = match(xor(0, 1), true)

    @Test
    fun testXor2() = match(xor(0, 2), false)

    @Test
    fun testXor3() = match(xor(1, 3), false)

    @Test
    fun testCombined() = match(and(or(0, 1), xor(2, 3)), true)

    @Test
    fun testCombined2() = match(and(or(0, 1), xor(2, 3)), true)

    @Test
    fun testCombined3() = match(or(and(0, 1), xor(1, 3)), false)

    @Test
    fun testCombined4() = match(or(and(0, 2), xor(1, 3)), true)

    @Test
    fun testCombined5() = match(xor(and(0, 1), or(1, 3)), false)

    @Test
    fun testCombined6() = match(xor(and(0, 2), or(1, 3)), true)

    private fun match(node: LogicalNode, expected: Boolean) {
        val evaluated = node.evaluate(placeholderValues)
        assertEquals(expected, evaluated)
    }

    private fun leaf(index: Int) = LogicalNode.Leaf(index)
    private fun and(left: LogicalNode, right: LogicalNode) = LogicalNode.And(left, right)
    private fun and(left: Int, right: Int) = and(leaf(left), leaf(right))
    private fun or(left: LogicalNode, right: LogicalNode) = LogicalNode.Or(left, right)
    private fun or(left: Int, right: Int) = or(leaf(left), leaf(right))
    private fun xor(left: LogicalNode, right: LogicalNode) = LogicalNode.Xor(left, right)
    private fun xor(left: Int, right: Int) = xor(leaf(left), leaf(right))
    private fun not(node: LogicalNode) = LogicalNode.Not(node)
    private fun not(index: Int) = not(leaf(index))
}
