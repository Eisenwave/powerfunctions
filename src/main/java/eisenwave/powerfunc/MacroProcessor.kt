package eisenwave.powerfunc

import java.io.*
import kotlin.system.exitProcess

class MacroProcessor(private val file: File, private val outputDir: File) {

    private val workingDir = File(".")
    private val metRequirements = HashSet<Requirement>()
    private val setupFile = File(outputDir, "_setup.mcfunction")

    var preserveFormatting = false

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
            val lines: List<String> = FileReader(file).use(FileReader::readLines)
            val outputFile = File(outputDir, withSuffix(file.toString(), "mcfunction"))

            parseLines(lines, outputFile)
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

    private fun parseLines(lines: List<String>, outputFile: File) {
        //val statements: List<MacroStatement> = lines.mapIndexed { index, line -> parseLine(index, line) }

        val outLines: MutableList<String> = ArrayList()

        var blockMode = false
        var blockPath = ""
        var blockHeadExp = emptyArray<String>()
        var blockRecursive = false
        val blockLines = ArrayList<String>()

        fun createBlock() {
            val blockHeadLines = blockHeadExp.run {
                this[lastIndex] = this[lastIndex].replace("\$_do", "function $NAMESPACE:$blockPath"); this
            }
            outLines += blockHeadLines
            if (blockRecursive)
                blockLines += blockHeadLines.map { "/$it" }
            val blockFile = File(outputDir, "$blockPath.mcfunction")
            parseLines(blockLines, blockFile)
            blockLines.clear()
        }

        outer@ for (index in 0..lines.lastIndex) {
            val line = lines[index]
            //println("$index=$line: blockMode=$blockMode")

            if (blockMode) when {
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
                    blockMode = false
                }
            }

            val statement = parseLine(index, line)
            if (!preserveFormatting && statement.type == StatementType.FORMATTING)
                continue@outer

            if (statement.isBlockHeader) {
                blockMode = true
                blockPath = "${outputFile.nameWithoutExtension}_${index + 1}"
                blockRecursive = statement.type == StatementType.WHILE
                blockHeadExp = statement.expand()
            } else outLines += statement.expand()

            for (requirement in statement.requirements) {
                //println("${statement.type}: $requirement)
                if (requirement !in metRequirements) {
                    //for (expanded in requirement.expand())
                    //    setupWriter.appendln(expanded)
                    metRequirements += requirement
                }
            }
        }

        // if in block and end of lines reached, create new block as well
        if (blockMode)
            createBlock()

        FileWriter(outputFile).use {
            for (line in outLines)
                it.appendln(line)
        }
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
