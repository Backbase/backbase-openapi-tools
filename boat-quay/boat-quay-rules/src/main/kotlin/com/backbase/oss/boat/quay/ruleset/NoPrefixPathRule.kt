package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.rule.api.*
import kotlin.collections.map
import kotlin.collections.orEmpty

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B007U",
    severity = Severity.MUST,
    title = "Unified Backbase API specs do NOT expect any prefix in paths"
)
class NoPrefixPathRule(config: Config) {

    private val validPathPrefixes = config
        .getStringList("PrefixPathRule.validPathPrefixes")
        .toSet()

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation>  =

        context.api.paths.orEmpty()
            .map {
                val extractParts = it.key.split("/")
                val prefix = if (extractParts.size > 1) extractParts[1] else ""
                Pair(prefix, it.value)
            }
            .filter {
                validPathPrefixes.contains(it.first)
            }
            .map {
                context.violation("Incorrect path prefix: ${it.first}", it.second)
            }

}
