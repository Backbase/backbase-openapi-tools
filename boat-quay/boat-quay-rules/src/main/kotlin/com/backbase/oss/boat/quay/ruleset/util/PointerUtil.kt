package com.backbase.oss.boat.quay.ruleset.util

import com.fasterxml.jackson.core.JsonPointer
import org.zalando.zally.core.plus
import org.zalando.zally.core.toEscapedJsonPointer
import org.zalando.zally.core.toJsonPointer

/**
 * Builds JSON pointers from the document structure. On OpenAPI 3.1 the parser can share one model object between
 * identical parts of the document, so `Context.violation(description, value)` may point at another location.
 */
object PointerUtil {

    fun pointer(base: JsonPointer, vararg tokens: Any): JsonPointer =
        tokens.fold(base) { pointer, token -> pointer + token.toString().toEscapedJsonPointer() }

    /**
     * Pointer below `/paths/{path}`, such as `/paths/~1contacts/get/responses/200`.
     */
    fun pathPointer(path: String, vararg tokens: Any): JsonPointer =
        pointer("/paths".toJsonPointer(), path, *tokens)
}
