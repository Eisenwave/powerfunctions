package eisenwave.powerfunc

import java.io.*
import kotlin.system.exitProcess

class MacroProcessor(private val file: File, private val outputDir: File) {

    private val workingDir = File(".")
    private val metRequirements = HashSet<Requirement>()
    private val setupFile = File(outputDir, "_setup.mcfunction")

    private var preserveFormatting = false
    private var chainX = 0

    init {
        if (!workingDir.isDirectory)
            throw FileNotFoundException("Working directory \"$workingDir\" is not a valid directory")
        if (!outputDir.isDirectory)
            throw FileNotFoundException("Output directory \"$outputDir\" is not a valid directory")
    }

    fun parse() {
        val parsableFiles: List<File> = if (file.isDirectory) {
            file.listFiles().filter { it.name.endsWith(".pf") }
        } else listOf(file)

        for (file in parsableFiles) {
            println("Parsing $file ...")
            val lines: List<String> = FileReader(file).use(FileReader::readLines)
            val outputFile = File(outputDir, withSuffix(file.toString(), "mcfunction"))

            parseLines(lines, outputFile, true)
        }

        val requirementsSorted = metRequirements.sortedWith(Comparator { a, b -> a.type.compareTo(b.type) })

        FileWriter(setupFile).use {
            for (requirement in requirementsSorted)
                for (expanded in requirement.expand())
                    it.appendln(expanded)
        }

        //println(metRequirements)

        //setupWriter.close()
    }

    /**
     * Parses a block consisting of multiple lines of statements and optionally writes the result into a specified
     * output file.
     *
     * @param lines the lines of code
     * @param outputFile the file into which to write the compiled code
     * @param noInline forces this layer of lines to not be inlined and instead be written to the file
     * @return the line to be inlined or null if the block can't be inlined
     */
    private fun parseLines(lines: List<String>, outputFile: File, noInline: Boolean): String? {
        //val statements: List<MacroStatement> = lines.mapIndexed { index, line -> parseLine(index, line) }

        val outLines: MutableList<String> = ArrayList()

        var parsingInBlockMode = false
        var blockPath = ""
        var blockHead: MacroStatement? = null
        val blockLines = ArrayList<String>()

        fun createBlock() {
            val type = blockHead!!.type
            val exp = blockHead!!.expand()
            val blockNoInline: Boolean

            // while statements generate recursive functions, so we need to add the recursive
            // callback to the block before we even parse it
            if (type == StatementType.WHILE) {
                exp[exp.lastIndex] = exp[exp.lastIndex].replace("\$_do", "function $NAMESPACE:$blockPath")
                blockLines += exp.map { "/$it" }
                blockNoInline = true
            } else blockNoInline = false

            // recursive call
            val inlinedBlock = parseLines(blockLines, File(outputDir, "$blockPath.mcfunction"), blockNoInline)
            val blockReplacement = inlinedBlock ?: "function $NAMESPACE:$blockPath"

            when (type.invocationType) {
                InvocationType.CALL_FUNCTION ->
                    exp[exp.lastIndex] = exp[exp.lastIndex].replace("\$_do", blockReplacement)

                InvocationType.SET_BLOCK -> {
                    exp[exp.lastIndex] = exp[exp.lastIndex].replace("\$_loc", "$chainX 1 1")
                    val zeroNBT = "{Command:\"setblock ~ ~ ~1 minecraft:stone\",Conditional:0b,auto:0b}"
                    val firstNBT = "{Command:\"$blockReplacement\",Conditional:0b,auto:0b}"
                    val chainNBT = "{Command:\"$blockReplacement\",Conditional:0b,auto:1b}"

                    metRequirements += BlockRequirement(BlockPos(chainX, 1, 0), "command_block", 3, zeroNBT)
                    metRequirements += BlockRequirement(BlockPos(chainX, 1, 2), "command_block", 3, firstNBT)
                    for (z in 3..(blockHead as ChainStatement).value + 1) {
                        metRequirements += BlockRequirement(BlockPos(chainX, 1, z), "chain_command_block", 3, chainNBT)
                    }
                    chainX++
                }
            }

            outLines += exp
            blockLines.clear()
        }

        outer@ for (index in 0..lines.lastIndex) {
            val line = lines[index]
            //println("$index=$line: blockMode=$blockMode")

            if (parsingInBlockMode) when {
                line.isBlank() -> {
                    if (preserveFormatting)
                        blockLines.add("")
                    continue@outer
                }
                line.startsWith(COMMENT_PREFIX) -> {
                    if (preserveFormatting)
                        blockLines.add(line)
                    continue@outer
                }
                line.startsWith("    ") -> {
                    blockLines.add(line.substring(4))
                    continue@outer
                }
                else -> {
                    createBlock()
                    parsingInBlockMode = false
                }
            }

            val statement = parseLine(index, line)
            if (!preserveFormatting && statement.type == StatementType.FORMATTING)
                continue@outer

            if (statement.type.isBlockHeader) {
                parsingInBlockMode = true
                blockPath = "${outputFile.nameWithoutExtension}_${index + 1}"
                blockHead = statement
            } else outLines += statement.expand()

            metRequirements += statement.requirements
        }

        // if in block and end of lines reached, create new block as well
        if (parsingInBlockMode)
            createBlock()

        return if (noInline || outLines.size > 1) {
            FileWriter(outputFile).use {
                for (line in outLines)
                    it.appendln(line)
            }
            null
        } else outLines.first()
    }

    private fun parseLine(num: Int, line: String): MacroStatement {
        val statement: MacroStatement
        try {
            statement = tokenizeLine(line)
        } catch (ex: MacroParseException) {
            error(num, line, ex)
            ex.printStackTrace()
            exitProcess(1)
        }
        return statement
    }

    @Suppress("UNUSED_PARAMETER")
    private fun error(lineNum: Int, line: String, ex: MacroParseException) {
        System.err.println("Error: ${ex.message}")
        System.err.println("  at ${file.name}: $lineNum")
    }

}
