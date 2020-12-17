package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class InfoBlockTitleCheckerTest {

    private val cut = InfoBlockTitleChecker(rulesConfig)


    @Test
    fun `correct value for title`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @Test
    fun `incorrect value for title max length`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod 
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `no value for title`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

    @Test
    fun `empty value for title`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: ''
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }

}