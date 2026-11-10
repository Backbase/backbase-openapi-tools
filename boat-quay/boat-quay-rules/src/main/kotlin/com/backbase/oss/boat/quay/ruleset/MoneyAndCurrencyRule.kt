package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.SchemaUtil
import com.typesafe.config.Config
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B023",
    severity = Severity.SHOULD,
    title = "Amounts are decimal strings and currencies are ISO 4217 codes"
)
class MoneyAndCurrencyRule(config: Config) {

    private val amountNamePattern = config.getString("${javaClass.simpleName}.amountNamePattern").toRegex()

    private val currencyNamePattern = config.getString("${javaClass.simpleName}.currencyNamePattern").toRegex()

    private val currencyCodePattern = config.getString("${javaClass.simpleName}.currencyCodePattern")

    /**
     * Object-typed `*Amount` and `*Currency` properties, such as the SSDK currency type, are not checked.
     */
    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
        SchemaUtil.namedProperties(context.api)
            .flatMap { property ->
                val name = property.name
                val resolved = SchemaUtil.resolve(property.schema, context.api.components)
                when {
                    amountNamePattern.matches(name) &&
                        (SchemaUtil.hasType(resolved, "number") || SchemaUtil.hasType(resolved, "integer")) -> listOf(
                        context.violation("Property $name should be a decimal string (type: string, format: number)", property.pointer)
                    )

                    currencyNamePattern.matches(name) && SchemaUtil.hasType(resolved, "string") -> listOfNotNull(
                        context.violation("Property $name should have pattern $currencyCodePattern", property.pointer)
                            .takeIf { resolved?.pattern != currencyCodePattern },
                        context.violation("Property $name should not list the allowed currencies in an enum", property.pointer)
                            .takeIf { !resolved?.enum.isNullOrEmpty() }
                    )

                    else -> emptyList()
                }
            }
}
