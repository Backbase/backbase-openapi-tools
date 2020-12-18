package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B006",
        severity = Severity.MUST,
        title = "Check info block description format."
)
class InfoBlockDescriptionChecker(config: Config) {

    private val maxDescriptionLength = config.getInt("InfoBlockDescriptionChecker.maxDescriptionLength")

    @Check(Severity.MUST)
    fun validate(context: Context) = when (val title = context.api.info.description) {
        null -> listOf(Violation("description is a required value", "/openapi/info/description".toJsonPointer()))
        ""  -> listOf(Violation("description can not be empty", "/openapi/info/description".toJsonPointer()))
        else -> if (title.length > maxDescriptionLength) listOf(Violation("description can not be longer than $maxDescriptionLength", "/openapi/info/description".toJsonPointer())) else emptyList()
    }

}