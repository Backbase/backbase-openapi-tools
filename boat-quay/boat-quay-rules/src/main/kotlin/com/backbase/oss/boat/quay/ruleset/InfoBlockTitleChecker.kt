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
    fun validate(context: Context): List<Violation> {
        val titlePointer = "/openapi/info/title".toJsonPointer()
        return when (val title = context.api.info.title) {
            null -> listOf(Violation("title is a required value", titlePointer))
            ""  -> listOf(Violation("title can not be empty", titlePointer))
            else -> if (title.length > maxTitleLength) listOf(Violation("title can not be longer than $maxTitleLength",
                titlePointer
            )) else emptyList()
        }
    }

}
