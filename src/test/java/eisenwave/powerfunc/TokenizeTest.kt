package eisenwave.powerfunc

import org.junit.Test
import org.junit.Assert.*

class TokenizeTest {

    @Test
    fun testTokenizeCommand() {
        val statement = tokenizeLine("/tp @a ~ ~ ~")
        assert(statement.type == StatementType.COMMAND)
    }

    @Test
    fun testTokenizeComment() {
        val statement = tokenizeLine("# this is a comment")
        assert(statement.type == StatementType.FORMATTING)
    }

    @Test
    fun testTokenizeNegate() {
        val shortStatement = tokenizeLine("negate  entity")
        val longStatement = tokenizeLine("negate    entity2     objective")

        assertTrue(shortStatement.type == StatementType.NEGATE)
        assertEquals("entity", (shortStatement as NegateStatement).player)
        assertEquals(INT32, shortStatement.objective)

        assertTrue(longStatement.type == StatementType.NEGATE)
        assertEquals("entity2", (longStatement as NegateStatement).player)
        assertEquals("objective", longStatement.objective)
    }

    @Test
    fun testTokenizeBoolify() {
        val shortStatement = tokenizeLine("boolify  entity")
        val longStatement = tokenizeLine("boolify    entity2     objective")

        assertTrue(shortStatement.type == StatementType.BOOLIFY)
        assertEquals("entity", (shortStatement as BoolifyStatement).player)
        assertEquals(INT32, shortStatement.objective)

        assertTrue(longStatement.type == StatementType.BOOLIFY)
        assertEquals("entity2", (longStatement as BoolifyStatement).player)
        assertEquals("objective", longStatement.objective)
    }

    @Test
    fun testTokenizeIfOperator() {
        val shortStatement = tokenizeLine("if x > y:")
        val longStatement = tokenizeLine("if @a[x=100] points != someone tarObject    :")

        assertTrue(shortStatement.type == StatementType.IF)
        assertEquals("x", (shortStatement as ConditionalOperatorStatement).lhs)
        assertEquals(INT32, shortStatement.lhsObjective)
        assertEquals(LogicalOperator.GT, shortStatement.op)
        assertEquals("y", shortStatement.rhs)
        assertEquals(INT32, shortStatement.rhsObjective)

        assertTrue(longStatement.type == StatementType.IF)
        assertEquals("@a[x=100]", (longStatement as ConditionalOperatorStatement).lhs)
        assertEquals("points", longStatement.lhsObjective)
        assertEquals(LogicalOperator.NEQ, longStatement.op)
        assertEquals("someone", longStatement.rhs)
        assertEquals("tarObject", longStatement.rhsObjective)
    }

    @Test
    fun testTokenizeChain() {
        val shortStatement = tokenizeLine("chain 100:")
        val longStatement = tokenizeLine("chain      94300674      :")

        assertTrue(shortStatement.type == StatementType.CHAIN)
        assertEquals(100, (shortStatement as ChainStatement).value)

        assertTrue(longStatement.type == StatementType.CHAIN)
        assertEquals(94300674, (longStatement as ChainStatement).value)
    }

    @Test
    fun testTokenizeLiteralAssignment() {
        val shortStatement = tokenizeLine("x = 7")
        val longStatement = tokenizeLine("@a[x=100] points = -262672962")

        assertTrue(shortStatement.type == StatementType.LITERAL_ASSIGNMENT)
        assertEquals("x", (shortStatement as LiteralAssignmentStatement).player)
        assertEquals(INT32, shortStatement.objective)
        assertEquals(7, shortStatement.value)

        assertTrue(longStatement.type == StatementType.LITERAL_ASSIGNMENT)
        assertEquals("@a[x=100]", (longStatement as LiteralAssignmentStatement).player)
        assertEquals("points", longStatement.objective)
        assertEquals(-262672962, longStatement.value)
    }

    @Test
    fun testTokenizeAssignmentOperation() {
        val shortStatement = tokenizeLine("x += 7")
        val longStatement = tokenizeLine("@a[x=100] points &&= someone tarObject")

        assertTrue(shortStatement.type == StatementType.ASSIGNMENT_OPERATION)
        assertEquals("x", (shortStatement as AssignmentOperationStatement).target)
        assertEquals(INT32, shortStatement.tarObjective)
        assertEquals(AssignmentOperator.ADD, shortStatement.op)
        assertEquals("7", shortStatement.source)
        assertEquals(INT32, shortStatement.srcObjective)

        assertTrue(longStatement.type == StatementType.ASSIGNMENT_OPERATION)
        assertEquals("@a[x=100]", (longStatement as AssignmentOperationStatement).target)
        assertEquals("points", longStatement.tarObjective)
        assertEquals(AssignmentOperator.ANDL, longStatement.op)
        assertEquals("someone", longStatement.source)
        assertEquals("tarObject", longStatement.srcObjective)
    }

}
