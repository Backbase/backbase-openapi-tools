package com.backbase.oss.boat.quay.ruleset

import org.zalando.zally.core.AbstractRuleSet
import org.zalando.zally.rule.api.Rule
import java.net.URI

class BoatRuleSet : AbstractRuleSet() {
    override val url: URI = URI.create("https://backbase.github.io/backbase-openapi-tools/rules.md")

    override fun url(rule: Rule): URI {
        val heading = "${rule.id}: ${rule.title}"
        val ref = heading
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
        return url.resolve("#$ref")
    }
}
