package com.backbase.oss.boat.quay.ruleset

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B009U",
    severity = Severity.MUST,
    title = "Check no prefix for paths should contain version (the rule intended for Unified Backbase API specs)"
)
class NoVersionInUriRule {

    private val description = "URL should not contain version number"
    private val versionRegex = "(.*)v[0-9]+(.*)".toRegex()

    @Check(severity = Severity.MUST)
    fun validate(context: Context): List<Violation> =
        (violatingPaths(context.api))
            .map { context.violation(description, it) }


    private fun violatingPaths(api: OpenAPI): Collection<PathItem> =
        api.paths.orEmpty().entries
            .filter { (path, _) -> path.matches(versionRegex) }
            .map { (_, pathEntry) -> pathEntry }
}