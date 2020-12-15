package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B004",
        severity = Severity.MUST,
        title = "Check info block title format."
)
class InfoBlockTitleChecker(config: Config) {

    private val MAX_TITLE_LENGTH = 35

    @Check(Severity.MUST)
    fun validate(context: Context) = when (val title = context.api.info.title) {
        null -> listOf(Violation("title is a required value", "/openapi/info/title".toJsonPointer()))
        ""  -> listOf(Violation("title can not be empty", "/openapi/info/title".toJsonPointer()))
        else -> if (title.length > MAX_TITLE_LENGTH) listOf(Violation("title can not be longer than $MAX_TITLE_LENGTH", "/openapi/info/title".toJsonPointer())) else emptyList()
    }

}