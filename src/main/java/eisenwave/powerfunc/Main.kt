package eisenwave.powerfunc

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println("Usage: powerfunc <file> <output directory>")
        exitProcess(1)
    }

    val file = File(args[0])
    val outputDir = File(args[1])

    MacroProcessor(file, outputDir).parse()
}
