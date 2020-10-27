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

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation> {

        val version = context.api.openapi;

        return when {
            !context.isOpenAPI3() -> emptyList()
            context.isOpenAPI3() && !openApiVersions.contains(version) ->
                listOf(Violation("OpenAPI specification version must be $openApiVersions. It's now set to `$version`" , "/openapi".toJsonPointer()))
            else -> emptyList()
        }
    }
}
