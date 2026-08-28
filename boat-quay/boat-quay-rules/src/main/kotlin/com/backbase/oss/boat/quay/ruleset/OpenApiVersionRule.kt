package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "M0012",
        severity = Severity.MUST,
        title = "Open API Version must be set to the correct version"
)
class OpenApiVersionRule(config: Config) {

    private val openApiVersions = config
            .getStringList("OpenApiVersionRule.openApiVersions")
            .toList()

    /**
     * Version patterns accepted in addition to the exact [openApiVersions] list, so that a whole minor line
     * (e.g. every 3.1.x patch release) can be allowed without enumerating each patch version.
     *
     * Read defensively: the key is absent from configurations written before it was introduced, and
     * `getStringList` throws on a missing path.
     */
    private val openApiVersionPatterns = if (config.hasPath("OpenApiVersionRule.openApiVersionPatterns")) {
        config.getStringList("OpenApiVersionRule.openApiVersionPatterns").map { it.toRegex() }
    } else {
        emptyList()
    }

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation> {

        if (!context.isOpenAPI3()) {
            return emptyList()
        }

        val version = context.api.openapi

        return if (isAccepted(version)) {
            emptyList()
        } else {
            listOf(Violation(
                    "OpenAPI specification version must be one of $openApiVersions" +
                            "${patternsSuffix()}. It's now set to `$version`",
                    "/openapi".toJsonPointer()))
        }
    }

    private fun isAccepted(version: String?): Boolean =
            version != null && (openApiVersions.contains(version)
                    || openApiVersionPatterns.any { version.matches(it) })

    private fun patternsSuffix(): String =
            if (openApiVersionPatterns.isEmpty()) "" else " or match one of $openApiVersionPatterns"
}
