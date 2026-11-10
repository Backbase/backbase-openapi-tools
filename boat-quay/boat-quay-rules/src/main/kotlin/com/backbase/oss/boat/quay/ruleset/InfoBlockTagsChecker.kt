package com.backbase.oss.boat.quay.ruleset

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import org.zalando.zally.core.toJsonPointer
import org.zalando.zally.rule.api.*

@Rule(
        ruleSet = BoatRuleSet::class,
        id = "B004",
        severity = Severity.MUST,
        title = "Check info block tags allowed."
)
class InfoBlockTagsChecker(config: Config) {

    private val productTags = config
            .getStringList("InfoBlockTagsChecker.productTags")
            .toSet()
    private val informativeTags = config
            .getStringList("InfoBlockTagsChecker.informativeTags")
            .toSet()

    private val objectMapper = ObjectMapper()

    /**
     * Validates the API portal tags in `info.x-api-dashboard-tags`, when present. Top-level `tags` group
     * operations and are not restricted.
     */
    @Check(Severity.MUST)
    fun validate(context: Context): List<Violation>  {
        val validTags = productTags.union(informativeTags)
        val dashboardTags = context.api.info?.extensions?.get(DASHBOARD_TAGS) ?: return emptyList()
        val pointer = "/info/$DASHBOARD_TAGS"
        val tags: JsonNode = objectMapper.valueToTree(dashboardTags)
        if (!tags.isArray) {
            return listOf(context.violation("$DASHBOARD_TAGS must be a list of tags with a name", pointer.toJsonPointer()))
        }
        return tags.mapIndexedNotNull { index, tag ->
            val name = tag.path("name").asText(null)
            when {
                name == null -> context.violation("tag name is required", "$pointer/$index".toJsonPointer())
                !validTags.contains(name) -> context.violation(
                    "tag name is not allowed: $name. Only $validTags are allowed",
                    "$pointer/$index".toJsonPointer()
                )
                else -> null
            }
        }
    }

    companion object {
        const val DASHBOARD_TAGS = "x-api-dashboard-tags"
    }
}
