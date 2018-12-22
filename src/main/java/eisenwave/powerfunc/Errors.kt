package eisenwave.powerfunc

import java.io.File
import java.lang.Exception

// EXCEPTIONS

open class MacroParseException : Exception {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

}

class MacroSyntaxException : MacroParseException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

}


data class FileStackElement(val file: File, val line: Int)
