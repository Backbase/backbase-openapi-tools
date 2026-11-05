package com.backbase.oss.boat.quay.ruleset.util

object PathUtil {

    private val slashes = "/+".toRegex()

    private val versionSegment = "v(\\d+)".toRegex()

    fun segments(path: String): List<String> = path.split(slashes).filter { it.isNotEmpty() }

    /**
     * Segments after the API type prefix (`client-api`, ...) and the version (`v1`). Paths of unified specs
     * have neither.
     */
    fun resourceSegments(path: String, prefixes: Collection<String>): List<String> {
        val segments = segments(path)
        val afterPrefix = if (segments.firstOrNull() in prefixes) segments.drop(1) else segments
        return if (afterPrefix.firstOrNull()?.matches(versionSegment) == true) afterPrefix.drop(1) else afterPrefix
    }

    /**
     * The `N` of the first `vN` segment, if any.
     */
    fun version(path: String): Int? =
        segments(path).firstNotNullOfOrNull { versionSegment.matchEntire(it) }?.groupValues?.get(1)?.toIntOrNull()
}
