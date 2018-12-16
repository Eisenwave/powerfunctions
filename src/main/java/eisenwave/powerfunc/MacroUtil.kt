package eisenwave.powerfunc

/**
 * Returns the suffix of a given path.
 *
 * @param path the path
 */
fun suffixOf(path: String): String? {
    //val name = file.name
    val index = path.lastIndexOf('.')

    return if (index == -1) null else path.substring(index + 1)
}

/**
 * Returns the given path with an alternative suffix.
 *
 * @param path the path
 * @param suffix the new suffix
 */
fun withSuffix(path: String, suffix: String): String {
    return withoutSuffix(path) + '.' + suffix
    /* return if (oldSuffix == null)
        "$path.$suffix"
    else
        "${path.substring(0, path.length - oldSuffix.length - 1)}.$suffix" */
}

fun withoutSuffix(path: String):String {
    return path.substringBeforeLast('.')
}
