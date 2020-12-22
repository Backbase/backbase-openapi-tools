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
        val s = "/openapi/info/title"
        return when (val title = context.api.info.title) {
            null -> listOf(Violation("title is a required value", s.toJsonPointer()))
            ""  -> listOf(Violation("title can not be empty", s.toJsonPointer()))
            else -> if (title.length > maxTitleLength) listOf(Violation("title can not be longer than $maxTitleLength", s.toJsonPointer())) else emptyList()
        }
    }

}
