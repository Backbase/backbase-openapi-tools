package com.backbase.oss.boat.quay.ruleset

import com.fasterxml.jackson.core.JsonPointer
import com.typesafe.config.Config
import org.zalando.zally.core.plus
import org.zalando.zally.core.util.PatternUtil
import org.zalando.zally.core.toEscapedJsonPointer
import org.zalando.zally.core.toJsonPointer
import com.backbase.oss.boat.quay.ruleset.util.WordUtil.isPlural
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B010",
        severity = Severity.HINT,
        title = "Pluralize Resource Names"
)
class PluralizeResourceNamesRule(rulesConfig: Config) {

    private val slash = "/"

    private val slashes = "/+".toRegex()

    @Suppress("SpreadOperator")
    internal val whitelist = mutableListOf(
            *rulesConfig
                    .getConfig(javaClass.simpleName)
                    .getStringList("whitelist")
                    .map { it.toRegex() }
                    .toTypedArray())

    @Check(severity = Severity.HINT)
    fun validate(context: Context): List<Violation> {
        return context.validatePaths { (path, _) ->
            pathSegments(sanitizedPath(path, whitelist))
                    .filter { isNonViolating(it) }
                    .map { violation(context, it, "/paths".toJsonPointer() + path.toEscapedJsonPointer()) }
        }
    }

    private fun sanitizedPath(path: String, regexList: List<Regex>): String {
        return regexList.fold("/$path/".replace(slashes, slash)) { updated, regex ->
            updated.replace(regex, slash)
        }
    }

    /**
     * path segments skipping first two segments (prefix and version).
     * `/client-api/v1/bars` client and v1 are ignored.
     */
    private fun pathSegments(path: String): List<String> {
        return path.split(slashes).filter { it.isNotEmpty() }.drop(2)
    }

    private fun isNonViolating(it: String) =
            !PatternUtil.isPathVariable(it) && !isPlural(it)

    private fun violation(context: Context, term: String, pointer: JsonPointer) =
            context.violation("Resource '$term' appears to be singular", pointer)

}