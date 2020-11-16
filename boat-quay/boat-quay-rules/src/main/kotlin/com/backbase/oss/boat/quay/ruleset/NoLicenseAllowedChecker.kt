package com.backbase.oss.boat.quay.ruleset

import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B001",
        severity = Severity.MUST,
        title = "No license information allowed because it's covered by the License Agreement we already negotiate with customers."
)
class NoLicenseAllowedChecker() {

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation> {

        return when {
            !context.isOpenAPI3() -> emptyList()
            context.api.info.license != null ->
                listOf(Violation("OpenAPI must not contain a license. ", "/openapi".toJsonPointer()))
            else -> emptyList()
        }
    }
}
