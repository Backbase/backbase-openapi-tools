package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B005",
        severity = Severity.MUST,
        title = "Check info block title format."
)
class InfoBlockTitleChecker(config: Config) {

    private val maxTitleLength = config.getInt("InfoBlockTitleChecker.maxTitleLength")

    @Check(Severity.MUST)
    fun validate(context: Context) = when (val title = context.api.info.title) {
        null -> listOf(Violation("title is a required value", "/openapi/info/title".toJsonPointer()))
        ""  -> listOf(Violation("title can not be empty", "/openapi/info/title".toJsonPointer()))
        else -> if (title.length > maxTitleLength) listOf(Violation("title can not be longer than $maxTitleLength", "/openapi/info/title".toJsonPointer())) else emptyList()
    }

}
