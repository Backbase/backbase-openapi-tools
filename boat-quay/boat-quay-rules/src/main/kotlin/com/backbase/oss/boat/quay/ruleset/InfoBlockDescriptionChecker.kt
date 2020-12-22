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
    fun validate(context: Context): List<Violation> {
        val descriptionPointer = "/openapi/info/description".toJsonPointer()
        return when (val title = context.api.info.description) {
            null -> listOf(Violation("description is a required value", descriptionPointer))
            ""  -> listOf(Violation("description can not be empty", descriptionPointer))
            else -> if (title.length > maxDescriptionLength) listOf(Violation("description can not be longer than $maxDescriptionLength",
                descriptionPointer
            )) else emptyList()
        }
    }

}