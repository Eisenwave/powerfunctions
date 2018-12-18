package eisenwave.powerfunc

import java.util.*

enum class RequirementType {
    OBJECTIVE,
    INIT_SCORE,
    COMMAND,
    BLOCK,
    LOGIC_ENTITY
}

// REQUIREMENTS

abstract class Requirement(val args: Array<String>) {

    abstract val type: RequirementType

    abstract fun expand(): Array<String>

    override fun toString(): String {
        return "$type${Arrays.toString(args)}"
    }

    override fun equals(other: Any?): Boolean {
        return other is Requirement
                && other.type == this.type
                && other.args.contentEquals(this.args)
    }

    override fun hashCode(): Int {
        return type.hashCode() xor Arrays.hashCode(args)
    }

}

class CommandRequirement(private val command: String) : Requirement(arrayOf(command)) {

    override val type = RequirementType.COMMAND

    override fun expand(): Array<String> {
        return arrayOf(command)
    }

}

class BlockRequirement(pos: BlockPos, private val id: String, private val data: Int, private val nbt: String?) :
        Requirement(arrayOf(pos.x.toString(), pos.y.toString(), pos.z.toString())) {

    constructor(pos: BlockPos, id: String) : this(pos, id, 0, null)

    override val type = RequirementType.BLOCK

    override fun expand(): Array<String> {
        return if (nbt == null) arrayOf(
                "setblock ${args[0]} ${args[1]} ${args[2]} minecraft:$id $data replace"
        )
        else arrayOf(
                "setblock ${args[0]} ${args[1]} ${args[2]} minecraft:air",
                "setblock ${args[0]} ${args[1]} ${args[2]} minecraft:$id $data replace $nbt"
                )
    }

}

class ObjectiveRequirement(objective: String, type: String) : Requirement(arrayOf(objective, type)) {

    constructor(objective: String) : this(objective, "dummy")

    override val type = RequirementType.OBJECTIVE

    override fun expand(): Array<String> {
        return arrayOf("scoreboard objectives add ${args[0]} ${args[1]}")
    }

}

class ScoreRequirement(entity: String, objective: String, value: Int) :
        Requirement(arrayOf(entity, objective, value.toString())) {

    // constructor(entity: String, value: Int) : this(entity, INT32, value)

    constructor(value: Int) : this("#$value", INT32, value)

    override val type = RequirementType.INIT_SCORE

    override fun expand(): Array<String> {
        return arrayOf("scoreboard players set ${args[0]} ${args[1]} ${args[2]}")
    }

}

class LogicEntityRequirement : Requirement(emptyArray()) {

    override val type = RequirementType.LOGIC_ENTITY

    override fun expand(): Array<String> {
        return arrayOf(
                "kill @e[tag=_logic]",
                "summon minecraft:armor_stand 0 0 0 {NoGravity:1,Marker:1,Invisible:1,Invulnerable:1,Tags:[_logic]}"
        )
    }

    override fun hashCode() = type.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is Requirement && other.type == this.type
    }
}
