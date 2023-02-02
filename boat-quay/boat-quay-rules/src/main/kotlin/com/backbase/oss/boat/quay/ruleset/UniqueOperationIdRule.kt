package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B002",
        severity = Severity.MUST,
        title = "All operationIds must be unique"
)
class UniqueOperationIdRule() {

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation> {

        val operationIds = mutableSetOf<String>()
        val violations = mutableListOf<Violation>()
        context.api.paths.orEmpty().values
                .flatMap { it?.readOperations().orEmpty() }
                .filter { operation ->
                    val exist = operationIds.contains(operation.operationId.lowercase())
                    if (!exist) {
                        operationIds.add(operation.operationId.lowercase())
                    }
                    exist;
                }
                .forEach { operation ->
                    val violation = context.violation("Operation has a duplicate name: ${operation.operationId}", operation);
                    violations.add(violation)
                }
        return violations
    }
}
