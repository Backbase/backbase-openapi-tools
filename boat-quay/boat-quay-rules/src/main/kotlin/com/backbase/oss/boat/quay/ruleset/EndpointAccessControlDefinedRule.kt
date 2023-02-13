package com.backbase.oss.boat.quay.ruleset

import com.fasterxml.jackson.core.JsonPointer
import com.typesafe.config.Config
import org.zalando.zally.rule.api.*
import java.nio.file.FileVisitOption
import java.util.Optional

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B013",
        severity = Severity.MUST,
        title = "Check access control is properly defined."
)
class EndpointAccessControlDefinedRule(config: Config) {

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation>  {

        fun notEmpty(s: Any?) : Boolean {
            return s != null && !s.toString().equals("")
        }

        val violations = mutableListOf<Violation>()
        context.api.paths.orEmpty().values
                .flatMap { it?.readOperations().orEmpty() }
                .forEach {
                    val acEnabledSet : Boolean = notEmpty(it.extensions["x-BBAccessControl"])
                    val acResourceSet : Boolean = notEmpty(it.extensions["x-BBAccessControl-resource"])
                    val acFunctionSet : Boolean = notEmpty(it.extensions["x-BBAccessControl-function"])
                    val acPrivilegeSet : Boolean = notEmpty(it.extensions["x-BBAccessControl-privilege"])
                    val acExplicitlyDisabled : Boolean = "false" == it.extensions["x-BBAccessControl"].toString()
                    if (acExplicitlyDisabled) {
                        if (acResourceSet || acFunctionSet || acPrivilegeSet) {
                            violations.add(
                                    context.violation("AC both disabled and defined: ${it.operationId}", it))
                        }
                    } else if (!acEnabledSet) {
                        if (!acResourceSet || !acFunctionSet || !acPrivilegeSet) {
                            violations.add(
                                    context.violation("AC info not complete: ${it.extensions}", it))
                        }
                    }
                }
        return violations
    }

}
