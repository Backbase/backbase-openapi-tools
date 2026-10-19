package com.backbase.oss.boat.quay.ruleset

import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B015",
    severity = Severity.MUST,
    title = "Check x-sunset-date is set and valid when x-deprecated is true"
)
class InfoBlockSunsetDateChecker {

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation> {
        val violations = mutableListOf<Violation>()
        val extensions = context.api.info.extensions ?: return emptyList()

        val isDeprecated = isTrue(extensions["x-deprecated"])
        val sunsetDateValue = extensions["x-sunset-date"]
        val hasSunsetDate = sunsetDateValue != null && sunsetDateValue.toString().isNotBlank()

        when {
            isDeprecated && !hasSunsetDate -> {
                violations.add(
                    context.violation(
                        "x-sunset-date must be set (as YYYY-MM-DD) when x-deprecated is true",
                        "/openapi/info/x-sunset-date".toJsonPointer()
                    )
                )
            }
            isDeprecated && hasSunsetDate -> {
                if (!isValidDate(sunsetDateValue.toString())) {
                    violations.add(
                        context.violation(
                            "x-sunset-date must be a valid YYYY-MM-DD date, got: ${sunsetDateValue}",
                            "/openapi/info/x-sunset-date".toJsonPointer()
                        )
                    )
                }
            }
            !isDeprecated && hasSunsetDate -> {
                violations.add(
                    context.violation(
                        "x-sunset-date is set but x-deprecated is not true — remove the stale sunset date or mark the API deprecated",
                        "/openapi/info/x-sunset-date".toJsonPointer()
                    )
                )
            }
        }

        return violations
    }

    private fun isTrue(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> false
        }
    }

    private fun isValidDate(value: String): Boolean {
        return try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }
}
