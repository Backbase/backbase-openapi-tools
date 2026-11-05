package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.PointerUtil
import com.typesafe.config.Config
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B016",
    severity = Severity.SHOULD,
    title = "Do not define 405, 408, 422 or 5xx responses"
)
class ForbiddenResponseCodesRule(config: Config) {

    private val forbidden = config
        .getStringList("${javaClass.simpleName}.forbidden")
        .map { it.toRegex(RegexOption.IGNORE_CASE) }

    /**
     * Any endpoint can return these, whether or not the spec defines them, and services should not return
     * 405, 408 or 422 themselves.
     */
    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
        context.validatePaths { (path, pathItem) ->
            pathItem?.readOperationsMap().orEmpty().flatMap { (method, operation) ->
                operation.responses.orEmpty().keys
                    .filter { status -> forbidden.any { it.matches(status) } }
                    .map { status ->
                        context.violation(
                            "$status response should not be defined",
                            PointerUtil.pathPointer(path, method.name.lowercase(), "responses", status)
                        )
                    }
            }
        }
}
