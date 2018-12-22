package eisenwave.powerfunc

import java.lang.AssertionError
import java.lang.Exception
import java.util.*
import java.util.regex.Pattern

const val INT32 = "_int32"
const val NAMESPACE = "power"
const val COMMENT_PREFIX = ";"

// private const val LITERAL_0 = "#0"
private const val LITERAL_1 = "#1"
private const val LITERAL_2 = "#2"
private const val PLAYER_TMP = "#_tmp"
private const val CMD_OPERATION = "scoreboard players operation"
private const val LOGIC_ENTITY = "@e[tag=_logic,c=1]"

private val REGEX_SPACES = Pattern.compile("[ ]+")

private val REGEX_DIGITS = Pattern.compile("-?[0-9]+")

// <target> [tarObj] <op> <source> [srcObj]
private val REGEX_ASSIGNMENT_OP = Pattern.compile("[^ ]+([ ]+[^ ]+)?[ ]+(\\+=|-=|\\*=|/=|%=|=|<|>|><|&=|&&=|\\|=|\\|\\|=|\\^=|===|<<=|>>=)[ ]+[^ ]+([ ]+[^ ]+)?")

private val REGEX_LITERAL_ASSIGNMENT = Pattern.compile("[^ ]+([ ]+[^ ]+)?[ ]+=[ ]+-?[0-9]+")

private fun isInteger(string: String) = REGEX_DIGITS.matcher(string).matches()

fun tokenizeLine(raw: String): MacroStatement {
    val string = raw.trimEnd()

    if (string.isBlank() || string.startsWith(COMMENT_PREFIX))
        return FormattingStatement(string)
    if (string.startsWith("/"))
        return CommandStatement(string, string.substring(1))

    val split = string.split(REGEX_SPACES)

    when (split[0]) {
        "absolute", "abs" -> {
            if (split.size < 2 || split.size > 3)
                throw MacroSyntaxException("Usage: abs[olute] <entity> [objective]")
            val objective = if (split.size == 3) split[2] else INT32
            return AbsoluteStatement(string, split[1], objective)
        }

        "negate", "neg" -> {
            if (split.size < 2 || split.size > 3)
                throw MacroSyntaxException("Usage: neg[ate] <entity> [objective]")
            val objective = if (split.size == 3) split[2] else INT32
            return NegateStatement(string, split[1], objective)
        }

        "boolify", "bool" -> {
            if (split.size < 2 || split.size > 3)
                throw MacroSyntaxException("Usage: bool[ify] <entity> [objective]")
            val objective = if (split.size == 3) split[2] else INT32
            return BoolifyStatement(string, split[1], objective)
        }

        "require", "req" -> return RequireCommandStatement(string, string.substring(split[0].length).trimStart())

        "require-objective", "req-obj" -> {
            if (split.size < 2 || split.size > 3)
                throw MacroSyntaxException("Usage: require-objective <objective> [type=dummy]")
            val objType = if (split.size == 3) split[2] else "dummy"
            return RequireObjectiveStatement(string, split[1], objType)
        }

        "require-score", "req-score" -> {
            if (split.size != 4)
                throw MacroSyntaxException("Usage: require-score <entity> <objective> <score>")
            val value = Integer.parseInt(split[3])
            return RequireScoreStatement(string, split[1], split[2], value)
        }

        "chain" -> {
            if (split.size < 2 || split.size > 3)
                throw MacroSyntaxException("Usage: chain <amount>:")
            var last = split.last()
            last = if (last == ":") {
                if (split.size == 2)
                    throw MacroSyntaxException("Usage: chain <amount>:")
                split[split.lastIndex - 1]
            } else {
                last.substring(0, last.length - 1)
            }

            val amount: Int
            try {
                amount = last.toInt()
            } catch (ex: NumberFormatException) {
                throw MacroSyntaxException("Amount \"$last\" is not a valid integer", ex)
            }

            return ChainStatement(string, amount)
        }

        "if", "while" -> {
            if (split.size < 4 || split.size > 7)
                throw MacroSyntaxException("Usage: ${split[0]} <entity> [objective] <operator> <entity> [objective]:")
            //val isLastColon: Boolean
            val actualLastIndex: Int
            var last = split.last()
            @Suppress("LiftReturnOrAssignment")
            if (last == ":") {
                if (split.size == 4)
                    throw MacroSyntaxException("Usage: ${split[0]} <entity> [objective] <operator> <entity> [objective]:")
                actualLastIndex = split.lastIndex - 1
                last = split[actualLastIndex]
            } else {
                actualLastIndex = split.lastIndex
                last = last.substring(0, last.length - 1)
                //isLastColon = false
            }


            var opIndex = -1
            var operator: LogicalOperator? = null

            for (i in 2..3) {
                operator = LogicalOperator.getByOpString(split[i])
                if (operator != null) {
                    opIndex = i
                    break
                }
            }

            if (opIndex == -1 || operator == null)
                throw AssertionError(string)

            val type = if (split[0] == "if") StatementType.IF else StatementType.WHILE
            val target: String = split[1]
            val tarObjective: String = if (opIndex == 3) split[2] else INT32
            val source: String = if (opIndex + 1 == actualLastIndex) last else split[opIndex + 1]
            val srcObjective = if (opIndex + 2 <= actualLastIndex) split[opIndex + 2] else INT32

            return ConditionalOperatorStatement(type, string, target, tarObjective, operator, source, srcObjective)
        }
    }

    if (REGEX_LITERAL_ASSIGNMENT.matcher(string).matches()) {
        val entity = split[0]
        val objective = if (split.size > 3) split[1] else INT32
        val value: Int
        try {
            value = Integer.parseInt(split.last())
        } catch (ex: NumberFormatException) {
            throw MacroSyntaxException("Literal value \"${split.last()}\" is not a valid integer", ex)
        }

        return LiteralAssignmentStatement(string, entity, objective, value)
    }

    if (REGEX_ASSIGNMENT_OP.matcher(string).matches()) {
        var opIndex = -1
        var operation: AssignmentOperator? = null
        for (i in 1..2) {
            operation = AssignmentOperator.getByOpString(split[i])
            if (operation != null) {
                opIndex = i
                break
            }
        }

        if (opIndex == -1 || operation == null)
            throw AssertionError(string)

        val target: String = split[0]
        val tarObjective: String = if (opIndex == 2) split[1] else INT32
        val source: String = split[opIndex + 1]
        val srcObjective = if (opIndex + 2 < split.size) split[opIndex + 2] else INT32

        return AssignmentOperationStatement(string, target, tarObjective, operation, source, srcObjective)
    }

    throw MacroSyntaxException("Can't process unrecognized macro\"$string\"")
}

// ENUMS

enum class InvocationType {
    SET_BLOCK,
    CALL_FUNCTION
}

enum class StatementType(val invocationType: InvocationType?) {
    EMPTY(null),
    // /...
    COMMAND(null),

    // comments, empty lines etc.
    FORMATTING(null),

    // macro command statements
    ABSOLUTE(null),
    NEGATE(null),
    BOOLIFY(null),

    // macro requirement commands
    REQUIRE(null),
    REQUIRE_OBJECTIVE(null),
    REQUIRE_SCORE(null),

    // code block statements
    IF(InvocationType.CALL_FUNCTION),
    WHILE(InvocationType.CALL_FUNCTION),
    CHAIN(InvocationType.SET_BLOCK),

    // variable operation statements
    ASSIGNMENT_OPERATION(null),
    LITERAL_ASSIGNMENT(null);

    val isBlockHeader = this.invocationType != null

}

enum class LogicalOperator(val string: String) {
    /** Greater than */
    GT(">"),
    /** Lower than */
    LT("<"),
    /** Greater-Equals */
    GE(">="),
    /** Lower-Equals */
    LE("<="),
    /** Equals */
    EQ("=="),
    /** Not-Equals */
    NEQ("!=");

    private lateinit var _flipped: LogicalOperator
    /**
     * The logical operator that results when flipping the left-hand side and the right-hand side.
     *
     * For instance, [GT] is converted into [LT] whereas [EQ] and [NEQ] stay unaffected.
     */
    val flipped
        get() = _flipped

    companion object {
        private val byOpString = HashMap<String, LogicalOperator>()

        init {
            for (op in values()) {
                byOpString[op.string] = op
                op._flipped = when (op) {
                    GT -> LT
                    LT -> GT
                    GE -> LE
                    LE -> GE
                    else -> op
                }
            }
        }

        fun getByOpString(op: String): LogicalOperator? {
            return byOpString[op]
        }
    }

    override fun toString(): String {
        return string
    }

}

enum class AssignmentOperator(val string: String) {
    /** Addition */
    ADD("+="),
    /** Subtraction */
    SUB("-="),
    /** Multiplication */
    MUL("*="),
    /** Division */
    DIV("/="),
    /** Modulo Division */
    MOD("%="),
    /** Move / Assignment */
    MOV("="),
    /** Minimum */
    MIN("<"),
    /** Maximum */
    MAX(">"),
    /** Swap */
    SWP("><"),
    /** Logical AND */
    AND("&="),
    /** Logical AND with boolified output */
    ANDL("&&="),
    /** Logical OR */
    OR("|="),
    /** Logical OR with boolified output */
    ORL("||="),
    /** Logical XOR */
    XOR("^="),
    /** Logical XOR wiht boolified output */
    XORL("^^="),
    /** Logical equality / XNOR with boolified output */
    EQ("==="),
    /** Left-shift */
    SHL("<<="),
    /** Right-shift */
    SHR(">>=")
    ;

    companion object {
        private val byOpString = HashMap<String, AssignmentOperator>()

        init {
            for (op in values()) {
                byOpString[op.string] = op
            }
        }

        fun getByOpString(op: String): AssignmentOperator? {
            return byOpString[op]
        }
    }

    override fun toString(): String {
        return string
    }

}

// STATEMENTS

abstract class MacroStatement(val raw: String) {

    //protected val raw = raw
    abstract val type: StatementType

    open val expands = true

    abstract val requirements: Array<out Requirement>

    open val warnings = emptyArray<String>()

    abstract fun expand(): Array<String>

}

class CommandStatement(raw: String, private val command: String) : MacroStatement(raw) {

    override val type = StatementType.COMMAND

    override val requirements = emptyArray<Requirement>()

    override fun expand(): Array<String> {
        return arrayOf(command)
    }

}

class FormattingStatement(raw: String) : MacroStatement(raw) {

    override val type = StatementType.FORMATTING

    override val requirements = emptyArray<Requirement>()

    override fun expand(): Array<String> {
        return arrayOf(raw)
    }

}

class NegateStatement(raw: String, val player: String, val objective: String) : MacroStatement(raw) {

    override val type = StatementType.NEGATE

    override val requirements = arrayOf(
            ObjectiveRequirement(INT32),
            ScoreRequirement(1),
            ScoreRequirement(2)
    )

    override fun expand(): Array<String> {
        return arrayOf(
                "$CMD_OPERATION $player $objective /= $player $objective",
                "$CMD_OPERATION $player $objective += $LITERAL_1 $INT32",
                "$CMD_OPERATION $player $objective %= $LITERAL_2 $INT32"
        )
    }

}

class BoolifyStatement(raw: String, val player: String, val objective: String) : MacroStatement(raw) {

    override val type = StatementType.BOOLIFY

    override val requirements = arrayOf(ObjectiveRequirement(INT32))

    override fun expand(): Array<String> {
        return arrayOf("$CMD_OPERATION $player $objective /= $player $objective")
    }

}

class AbsoluteStatement(raw: String, private val player: String, private val objective: String) : MacroStatement(raw) {

    override val type = StatementType.ABSOLUTE

    override val requirements = arrayOf(
            ObjectiveRequirement(INT32),
            ScoreRequirement(-1),
            LogicEntityRequirement()
    )

    override fun expand(): Array<String> {
        return arrayOf(
                "$CMD_OPERATION @e[tag=_logic,c=1] $INT32 = $PLAYER_TMP $INT32",
                "execute @e[tag=_logic,c=1,${INT32}_max=-1] ~ ~ ~ $CMD_OPERATION $player $objective *= #-1 $INT32"
        )
    }

}

class RequireCommandStatement(raw: String, command: String) : MacroStatement(raw) {

    override val type = StatementType.REQUIRE
    override val expands = false

    override val requirements = arrayOf(CommandRequirement(command))

    override fun expand() = emptyArray<String>()

}

class RequireObjectiveStatement(raw: String, objective: String, objectiveType: String) : MacroStatement(raw) {

    override val type = StatementType.REQUIRE_OBJECTIVE
    override val expands = false

    override val requirements = arrayOf(ObjectiveRequirement(objective, objectiveType))

    override fun expand() = emptyArray<String>()

}

class RequireScoreStatement(raw: String, entity: String, objective: String, value: Int) : MacroStatement(raw) {

    override val type = StatementType.REQUIRE_OBJECTIVE
    override val expands = false

    override val requirements = arrayOf(ScoreRequirement(entity, objective, value))

    override fun expand() = emptyArray<String>()

}

class LiteralAssignmentStatement(raw: String, val player: String, val objective: String, val value: Int) :
        MacroStatement(raw) {

    override val type = StatementType.LITERAL_ASSIGNMENT

    override val requirements = emptyArray<Requirement>()

    override fun expand(): Array<String> {
        return arrayOf("scoreboard players set $player $objective $value")
    }

}

class ChainStatement(raw: String, val value: Int) : MacroStatement(raw) {

    override val type = StatementType.CHAIN
    override val expands = true

    override val requirements = emptyArray<Requirement>()

    override fun expand() = arrayOf(
            "setblock \$_loc minecraft:redstone_block"
    )

}

class ConditionalOperatorStatement(override val type: StatementType, raw: String,
                                   lhs: String, lhsObjective: String,
                                   op: LogicalOperator,
                                   rhs: String, rhsObjective: String) : MacroStatement(raw) {

    private val lhsConstant: Int?
    private val rhsConstant: Int?

    val lhs: String
    val lhsObjective: String
    val rhs: String
    val rhsObjective: String
    val op: LogicalOperator

    /*
     * Handle constant cases. If lhs is a constant and rhs isn't, flip sides so that rhs is guaranteed to be the
     * constant in the operation.
     *
     * Remember which sides were constants so that the expression can be potentially evaluated by the compiler
     */
    init {
        when {
            isInteger(rhs) -> {
                this.rhs = "#$rhs"
                this.rhsObjective = rhsObjective
                this.rhsConstant = Integer.parseInt(rhs)
                this.op = op
                if (isInteger(lhs)) {
                    this.lhsConstant = Integer.parseInt(lhs)
                    this.lhs = "#$lhs"
                } else {
                    this.lhsConstant = null
                    this.lhs = lhs
                }
                this.lhsObjective = lhsObjective
            }
            isInteger(lhs) -> {
                this.rhs = "#$lhs"
                this.rhsObjective = lhsObjective
                this.rhsConstant = Integer.parseInt(lhs)
                this.op = op.flipped
                this.lhs = rhs
                this.lhsObjective = rhsObjective
                this.lhsConstant = null
            }
            else -> {
                this.rhs = rhs
                this.rhsObjective = rhsObjective
                this.rhsConstant = null
                this.op = op
                this.lhs = lhs
                this.lhsConstant = null
                this.lhsObjective = lhsObjective
            }
        }
    }

    override val requirements = when {
        rhsConstant != null && lhsConstant != null -> arrayOf(
                ObjectiveRequirement(INT32),
                LogicEntityRequirement(),
                ScoreRequirement(this.lhs, this.lhsObjective, this.lhsConstant),
                ScoreRequirement(this.rhs, this.rhsObjective, this.rhsConstant)
        )

        rhsConstant != null -> arrayOf(
                ObjectiveRequirement(INT32),
                LogicEntityRequirement(),
                ScoreRequirement(this.rhs, this.rhsObjective, this.rhsConstant)
        )

        lhsConstant != null -> arrayOf(
                ObjectiveRequirement(INT32),
                LogicEntityRequirement(),
                ScoreRequirement(this.lhs, this.lhsObjective, this.lhsConstant)
        )

        else -> arrayOf(
                ObjectiveRequirement(INT32),
                LogicEntityRequirement()
        )
    }

    override val warnings = if (lhsConstant != null)
        arrayOf("Pointless constant expression $lhs $op $rhs")
    else
        emptyArray()

    override fun expand(): Array<String> {
        return if (rhsConstant == null) {
            when (op) {
                LogicalOperator.GT -> arrayOf(
                        "$CMD_OPERATION $PLAYER_TMP $INT32 = $lhs $lhsObjective",
                        "$CMD_OPERATION $PLAYER_TMP $INT32 -= $rhs $rhsObjective",
                        "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $PLAYER_TMP $INT32",
                        "execute @e[tag=_logic,c=1,score_${INT32}_min=1] ~ ~ ~ \$_do"
                )

                LogicalOperator.LT -> arrayOf(
                        "$CMD_OPERATION $PLAYER_TMP $INT32 = $lhs $lhsObjective",
                        "$CMD_OPERATION $PLAYER_TMP $INT32 -= $rhs $rhsObjective",
                        "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $PLAYER_TMP $INT32",
                        "execute @e[tag=_logic,c=1,score_$INT32=-1] ~ ~ ~ \$_do"
                )

                LogicalOperator.GE -> arrayOf(
                        "$CMD_OPERATION $PLAYER_TMP $INT32 = $lhs $lhsObjective",
                        "$CMD_OPERATION $PLAYER_TMP $INT32 -= $rhs $rhsObjective",
                        "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $PLAYER_TMP $INT32",
                        "execute @e[tag=_logic,c=1,score_${INT32}_min=0] ~ ~ ~ \$_do"
                )

                LogicalOperator.LE -> arrayOf(
                        "$CMD_OPERATION $PLAYER_TMP $INT32 = $lhs $lhsObjective",
                        "$CMD_OPERATION $PLAYER_TMP $INT32 -= $rhs $rhsObjective",
                        "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $PLAYER_TMP $INT32",
                        "execute @e[tag=_logic,c=1,score_$INT32=0] ~ ~ ~ \$_do"
                )

                LogicalOperator.EQ -> arrayOf(
                        "$CMD_OPERATION $PLAYER_TMP $INT32 = $lhs $lhsObjective",
                        "$CMD_OPERATION $PLAYER_TMP $INT32 -= $rhs $rhsObjective",
                        "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $PLAYER_TMP $INT32",
                        "execute @e[tag=_logic,c=1,score_$INT32=0,score_${INT32}_min=0] ~ ~ ~ \$_do"
                )

                LogicalOperator.NEQ -> arrayOf(
                        "$CMD_OPERATION $PLAYER_TMP $INT32 = $lhs $lhsObjective",
                        "$CMD_OPERATION $PLAYER_TMP $INT32 -= $rhs $rhsObjective",
                        "$CMD_OPERATION $PLAYER_TMP $INT32 /= $PLAYER_TMP $INT32",
                        "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $PLAYER_TMP $INT32",
                        "execute @e[tag=_logic,c=1,score_${INT32}_min=1] ~ ~ ~ \$_do"
                )
            }
        } else when (op) {
            LogicalOperator.GT -> arrayOf(
                    "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $lhs $lhsObjective",
                    "execute @e[tag=_logic,c=1,score_${INT32}_min=${rhsConstant + 1}] ~ ~ ~ \$_do"
            )

            LogicalOperator.LT -> arrayOf(
                    "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $lhs $lhsObjective",
                    "execute @e[tag=_logic,c=1,score_$INT32=${rhsConstant - 1}] ~ ~ ~ \$_do"
            )

            LogicalOperator.GE -> arrayOf(
                    "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $lhs $lhsObjective",
                    "execute @e[tag=_logic,c=1,score_${INT32}_min=$rhsConstant] ~ ~ ~ \$_do"
            )

            LogicalOperator.LE -> arrayOf(
                    "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $lhs $lhsObjective",
                    "execute @e[tag=_logic,c=1,score_$INT32=$rhsConstant] ~ ~ ~ \$_do"
            )

            LogicalOperator.EQ -> {
                val sel = "@e[tag=_logic,c=1,score_${INT32}_min=$rhsConstant,score_$INT32=$rhsConstant]"
                arrayOf(
                        "$CMD_OPERATION $LOGIC_ENTITY $INT32 = $lhs $lhsObjective",
                        "execute $sel ~ ~ ~ \$_do"
                )
            }

            LogicalOperator.NEQ -> arrayOf(
                    "$CMD_OPERATION $PLAYER_TMP $INT32 = $lhs $lhsObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 /= $PLAYER_TMP $INT32",
                    "execute @e[tag=_logic,c=1,score_${INT32}_min=1] ~ ~ ~ \$_do"
            )
        }
    }

}

class AssignmentOperationStatement(raw: String,
                                   val target: String, val tarObjective: String,
                                   val op: AssignmentOperator,
                                   val source: String, val srcObjective: String) : MacroStatement(raw) {

    override val type = StatementType.ASSIGNMENT_OPERATION

    override val requirements
        get(): Array<out Requirement> {
            val list = ArrayList<Requirement>(4)
            list += ObjectiveRequirement(INT32)

            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (op) {
                AssignmentOperator.SHR, AssignmentOperator.SHL -> {
                    val num = Math.pow(2.0, Integer.parseInt(source).toDouble()).toInt()
                    list += ScoreRequirement("#$num", srcObjective, num)
                }

                else -> {
                    if (op == AssignmentOperator.XOR)
                        list += ScoreRequirement(2)
                    else if (isInteger(source))
                        list += ScoreRequirement("#$source", srcObjective, Integer.parseInt(source))
                }
            }

            return list.toTypedArray()
        }

    override fun expand(): Array<String> {
        val source: String = if (op != AssignmentOperator.SHL && op != AssignmentOperator.SHR && isInteger(source))
            "#$source"
        else
            this.source

        when (op) {
            AssignmentOperator.ADD -> return arrayOf("$CMD_OPERATION $target $tarObjective += $source $srcObjective")
            AssignmentOperator.SUB -> return arrayOf("$CMD_OPERATION $target $tarObjective -= $source $srcObjective")
            AssignmentOperator.MUL -> return arrayOf("$CMD_OPERATION $target $tarObjective *= $source $srcObjective")
            AssignmentOperator.DIV -> return arrayOf("$CMD_OPERATION $target $tarObjective /= $source $srcObjective")
            AssignmentOperator.MOD -> return arrayOf("$CMD_OPERATION $target $tarObjective %= $source $srcObjective")
            AssignmentOperator.MIN -> return arrayOf("$CMD_OPERATION $target $tarObjective < $source $srcObjective")
            AssignmentOperator.MAX -> return arrayOf("$CMD_OPERATION $target $tarObjective > $source $srcObjective")
            AssignmentOperator.SWP -> return arrayOf("$CMD_OPERATION $target $tarObjective >< $source $srcObjective")
            AssignmentOperator.MOV -> return arrayOf("$CMD_OPERATION $target $tarObjective = $source $srcObjective")

            // target *= source
            AssignmentOperator.AND -> return arrayOf("$CMD_OPERATION $target $tarObjective *= $source $srcObjective")

            // target *= source
            // target /= target
            AssignmentOperator.ANDL -> return arrayOf(
                    "$CMD_OPERATION $target $tarObjective *= $source $srcObjective",
                    "$CMD_OPERATION $target $tarObjective /= $target $tarObjective"
            )

            // target -= source
            // target /= target
            AssignmentOperator.EQ -> return arrayOf(
                    "$CMD_OPERATION $target $tarObjective -= $source $srcObjective",
                    "$CMD_OPERATION $target $tarObjective /= $target $tarObjective"
            )

            // target /= target
            // _tmp = source
            // _tmp /= tmp
            // target += _tmp
            AssignmentOperator.OR -> return arrayOf(
                    "$CMD_OPERATION $target $tarObjective /= $target $tarObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 = $source $srcObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 /= $PLAYER_TMP $INT32",
                    "$CMD_OPERATION $target $tarObjective += $PLAYER_TMP $INT32",
                    "$CMD_OPERATION $target $tarObjective /= $target $INT32"
            )

            // target /= target
            // _tmp = source
            // _tmp /= tmp
            // target += _tmp
            // target /= target
            AssignmentOperator.ORL -> return arrayOf(
                    "$CMD_OPERATION $target $tarObjective /= $target $tarObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 = $source $srcObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 /= $PLAYER_TMP $INT32",
                    "$CMD_OPERATION $target $tarObjective += $PLAYER_TMP $INT32",
                    "$CMD_OPERATION $target $tarObjective /= $target $INT32"
            )

            // target /= _target
            // _tmp = source
            // _tmp /= _tmp
            // target -= _tmp
            AssignmentOperator.XOR -> return arrayOf(
                    "$CMD_OPERATION $target $tarObjective /= $target $tarObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 = $source $srcObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 /= $PLAYER_TMP $INT32",
                    "$CMD_OPERATION $target $tarObjective -= $PLAYER_TMP $INT32"
            )

            // target /= _target
            // _tmp = source
            // _tmp /= _tmp
            // target += _tmp
            // target %= 2
            AssignmentOperator.XORL -> return arrayOf(
                    "$CMD_OPERATION $target $tarObjective /= $target $tarObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 = $source $srcObjective",
                    "$CMD_OPERATION $PLAYER_TMP $INT32 /= $PLAYER_TMP $INT32",
                    "$CMD_OPERATION $target $tarObjective += $PLAYER_TMP $INT32",
                    "$CMD_OPERATION $target $tarObjective %= $LITERAL_2 $INT32"
            )

            AssignmentOperator.SHL -> {
                val bits = Integer.parseInt(this.source)
                val factor = Math.pow(2.0, bits.toDouble()).toInt()
                return arrayOf("$CMD_OPERATION $target $tarObjective *= #$factor $srcObjective")
            }

            AssignmentOperator.SHR -> {
                val bits = Integer.parseInt(this.source)
                val factor = Math.pow(2.0, bits.toDouble()).toInt()
                return arrayOf("$CMD_OPERATION $target $tarObjective /= #$factor $srcObjective")
            }

        }
    }

}
