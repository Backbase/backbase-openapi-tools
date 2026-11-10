package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.util.PathUtil
import com.backbase.oss.boat.quay.ruleset.util.PointerUtil
import org.zalando.zally.rule.api.*

@Rule(
    ruleSet = BoatRuleSet::class,
    id = "B018",
    severity = Severity.SHOULD,
    title = "Path version matches the major version of the spec"
)
class PathVersionMatchesMajorRule {

    private val majorVersion = "^v?(\\d+)".toRegex()

    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> {
        val version = context.api.info?.version ?: return emptyList()
        val major = majorVersion.find(version)?.groupValues?.get(1)?.toIntOrNull() ?: return emptyList()
        return context.validatePaths { (path, _) ->
            val pathVersion = PathUtil.version(path)
            if (pathVersion == null || pathVersion == major) {
                emptyList()
            } else {
                listOf(
                    context.violation(
                        "Path version v$pathVersion does not match major version $major of info.version $version",
                        PointerUtil.pathPointer(path)
                    )
                )
            }
        }
    }
}
