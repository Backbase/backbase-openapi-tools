package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.PathUtil
import com.backbase.oss.boat.quay.ruleset.util.PointerUtil
import com.typesafe.config.Config
import org.zalando.zally.core.util.PatternUtil
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B017",
    severity = Severity.SHOULD,
    title = "Do not use verbs in paths"
)
class NoVerbsInPathRule(config: Config) {

    private val prefixes = config.getStringList("PrefixPathRule.validPathPrefixes")

    private val verbs = config.getStringList("${javaClass.simpleName}.verbs")

    private val verbsAllowedAfter = config.getStringList("${javaClass.simpleName}.verbsAllowedAfter").toSet()

    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
        context.validatePaths { (path, _) ->
            PathUtil.resourceSegments(path, prefixes)
                .takeWhile { it !in verbsAllowedAfter }
                .filterNot { PatternUtil.isPathVariable(it) }
                .filter { isVerb(it) }
                .map { context.violation("Path segment '$it' is a verb, use the HTTP method instead", PointerUtil.pathPointer(path)) }
        }

    /**
     * `update`, `update-status` and `updateStatus` start with the verb `update`; `updates` and `settings` do not.
     */
    private fun isVerb(segment: String): Boolean = verbs.any { verb ->
        segment == verb ||
            segment.startsWith("$verb-") ||
            (segment.startsWith(verb) && segment.length > verb.length && segment[verb.length].isUpperCase())
    }
}
