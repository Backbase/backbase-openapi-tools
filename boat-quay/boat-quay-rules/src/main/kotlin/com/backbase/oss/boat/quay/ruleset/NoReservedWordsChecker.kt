package com.backbase.oss.boat.quay.ruleset

import org.zalando.zally.core.toJsonPointer
import com.typesafe.config.Config
import org.zalando.zally.core.util.getAllProperties
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B003",
        severity = Severity.MUST,

        title = "No reserved words allowed."
)
class NoReservedWordsChecker(config: Config) {

    private val jsReservedWords = config
            .getStringList("NoReservedWordsChecker.jsReservedWords")
            .toSet()
    private val springReservedWords = config
            .getStringList("NoReservedWordsChecker.springReservedWords")
            .toList()
    private val kotlinReservedWords = config
            .getStringList("NoReservedWordsChecker.kotlinReservedWords")
            .toList()
    private val swiftReservedWords = config
            .getStringList("NoReservedWordsChecker.swiftReservedWords")
            .toList()
     private val reservedWords = jsReservedWords.union(springReservedWords).union(kotlinReservedWords).union(swiftReservedWords);

    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation> =
            context.api.getAllProperties()
                    .filter { schema ->
                        when {
                            reservedWords.contains(schema.key) -> true
                            else -> false
                        }
                    }
                    .map {
                        context.violation("Property uses a reserved word: ${it.key}", it.value);
                    }

}
