package com.backbase.oss.boat.quay.ruleset.util

import javatools.parsers.PlingStemmer

object WordUtil {

    private val PLURAL_WHITELIST = setOf("portal", "apis")

    fun isPlural(word: String): Boolean = PLURAL_WHITELIST.contains(word) || PlingStemmer.isPlural(word)
}