package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.core.util.getAllProperties
import org.zalando.zally.rule.api.Check
import org.zalando.zally.rule.api.Context
import org.zalando.zally.rule.api.Rule
import org.zalando.zally.rule.api.Severity
import org.zalando.zally.rule.api.Violation
import io.swagger.v3.oas.models.media.Schema

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "S007",
    severity = Severity.SHOULD,
    title = "Define bounds for lengths of string properties"
)
class StringPropertyLengthBoundsRule(config: Config) {

    internal val formatWhitelist = config
        .getStringList("StringPropertyLengthBoundsRule.formatWhitelist")
        .toList()

    internal val patternImpliesLimits = config
        .getBoolean("StringPropertyLengthBoundsRule.patternImpliesLimits")

    @Check(severity = Severity.SHOULD)
    fun checkStringLengthBounds(context: Context): List<Violation> =
        context.api
            .getAllProperties()
            .filterValues { schema ->
                when {
                    schema.type != "string" -> false
                    schema.format in formatWhitelist -> false
                    schema.pattern != null && patternImpliesLimits -> false
                    schema.enum != null -> false
                    else -> true
                }
            }
            .flatMap { (_, schema) ->
                checkStringLengthBoundsReversed(context, schema) +
                    checkStringLengthBound(context, "minLength", schema.minLength, schema) +
                    checkStringLengthBound(context, "maxLength", schema.maxLength, schema)
            }

    private fun checkStringLengthBoundsReversed(context: Context, schema: Schema<Any>): List<Violation> =
        when {
            schema.minLength != null &&
                schema.maxLength != null &&
                schema.minLength > schema.maxLength
            -> context.violations("minLength > maxLength is invalid", schema)
            else -> emptyList()
        }

    private fun checkStringLengthBound(context: Context, bound: String, value: Int?, schema: Schema<Any>) =
        when {
            value == null -> context.violations("No $bound defined", schema)
            value < 0 -> context.violations("Negative $bound is invalid", schema)
            else -> emptyList()
        }
}
