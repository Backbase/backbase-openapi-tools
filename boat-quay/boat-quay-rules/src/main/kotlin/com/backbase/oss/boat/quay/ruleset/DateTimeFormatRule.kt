package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.SchemaUtil
import com.typesafe.config.Config
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B022",
    severity = Severity.SHOULD,
    title = "Date and time properties declare a date-time, date or time format"
)
class DateTimeFormatRule(config: Config) {

    private val namePattern = config.getString("${javaClass.simpleName}.namePattern").toRegex()

    private val formats = config.getStringList("${javaClass.simpleName}.formats")

    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
        SchemaUtil.namedProperties(context.api)
            .filter { property ->
                val resolved = SchemaUtil.resolve(property.schema, context.api.components)
                namePattern.matches(property.name) && SchemaUtil.hasType(resolved, "string") && resolved?.format !in formats
            }
            .map { property ->
                context.violation("Property ${property.name} should have format ${formats.joinToString(" or ")}", property.pointer)
            }
}
