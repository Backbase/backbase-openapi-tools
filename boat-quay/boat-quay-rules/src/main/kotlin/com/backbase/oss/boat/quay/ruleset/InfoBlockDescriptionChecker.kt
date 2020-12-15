package com.backbase.oss.boat.quay.ruleset

import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B004",
        severity = Severity.MUST,
        title = "Check info block title format."
)
class InfoBlockDescriptionChecker {

    private val MAX_TITLE_LENGTH = 140

    @Check(Severity.MUST)
    fun validate(context: Context) = when (val title = context.api.info.description) {
        null -> listOf(Violation("description is a required value", "/openapi/info/description".toJsonPointer()))
        ""  -> listOf(Violation("description can not be empty", "/openapi/info/description".toJsonPointer()))
        else -> if (title.length > MAX_TITLE_LENGTH) listOf(Violation("description can not be longer than $MAX_TITLE_LENGTH", "/openapi/info/description".toJsonPointer())) else emptyList()
    }

}