package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class InfoBlockTagsCheckerTest {

    private val cut = InfoBlockTagsChecker(rulesConfig)

    @Test
    fun `incorrect value for tag`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            tags:
              - name: rendition service
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `correct value for tag`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            tags:
              - name: Retail
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }
}