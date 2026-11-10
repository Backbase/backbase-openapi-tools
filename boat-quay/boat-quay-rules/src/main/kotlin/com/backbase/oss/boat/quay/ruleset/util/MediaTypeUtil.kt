package com.backbase.oss.boat.quay.ruleset.util

object MediaTypeUtil {

    private const val APPLICATION_JSON = "application/json"

    /**
     * `application/json` and structured syntax types such as `application/problem+json`.
     */
    fun isJson(mediaType: String): Boolean = baseType(mediaType).let { it == APPLICATION_JSON || it.endsWith("+json") }

    fun isApplicationJson(mediaType: String): Boolean = baseType(mediaType) == APPLICATION_JSON

    fun hasParameters(mediaType: String): Boolean = mediaType.contains(";")

    private fun baseType(mediaType: String) = mediaType.substringBefore(";").trim().lowercase()
}
