package com.backbase.oss.boat.quay.ruleset

import com.typesafe.config.Config
import org.zalando.zally.rule.api.*

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
                    if (it.extensions == null) {
                        violations.add(
                                context.violation("Access Control not defined: ${it.operationId}", it))
                    } else {
                        val acExplicitlyDisabled: Boolean = "false" == it.extensions["x-BbAccessControl"].toString()
                        val acEnabledSet: Boolean = notEmpty(it.extensions["x-BbAccessControl"])
                        val isMultipleAcs: Boolean = notEmpty(it.extensions["x-BbAccessControls"])
                        val acResourceSet: Boolean = notEmpty(it.extensions["x-BbAccessControl-resource"])
                        val acFunctionSet: Boolean = notEmpty(it.extensions["x-BbAccessControl-function"])
                        val acPrivilegeSet: Boolean = notEmpty(it.extensions["x-BbAccessControl-privilege"])

                        if (!acExplicitlyDisabled && isMultipleAcs) {
                            val entries: List<*> = it.extensions["x-BbAccessControls"] as List<*>;
                            val numberOfEntries = entries.size;

                            if (numberOfEntries == 0) {
                                violations.add(context.violation("No permissions defined despite presence of x-BbAccessControls", it))
                            }

                            entries.forEach {
                                val entry: Map<String, String> = it as Map<String, String>;

                                if (!notEmpty(entry["resource"]) || !notEmpty(entry["function"]) || !notEmpty(entry["privilege"])) {
                                    violations.add(context.violation("AC Permission must contain resource, function and privilege parameters", it))
                                }

                                if (entry.size > 3) {
                                    violations.add(context.violation("AC Permission contains invalid parameter", it))
                                }
                            }

                        } else if (acExplicitlyDisabled && (acResourceSet || acFunctionSet || acPrivilegeSet || isMultipleAcs)) {
                            violations.add(
                                    context.violation("AC both disabled and defined: ${it.operationId}", it))

                        } else if (!acEnabledSet && (!acResourceSet || !acFunctionSet || !acPrivilegeSet)) {
                            violations.add(
                                    context.violation("AC info not complete: ${it.extensions}", it))
                        }
                    }
                }

        return violations
    }
}

