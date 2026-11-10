package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class InfoBlockTagsCheckerTest {

    private val cut = InfoBlockTagsChecker(rulesConfig)

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `incorrect value for dashboard tag`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
              x-api-dashboard-tags:
                - name: Retail
                - name: rendition service
                - description: no name
            paths: {}
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .descriptionsEqualTo(
                    "tag name is not allowed: rendition service. Only [Retail, Business, Wealth, Identity, Foundation, Basic Support, Flow, Mobile, Security, Payments, Authentication, Employee, Cash, Insights] are allowed",
                    "tag name is required"
                )
                .pointersEqualTo("/info/x-api-dashboard-tags/1", "/info/x-api-dashboard-tags/2")
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `correct value for dashboard tag`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
              x-api-dashboard-tags:
                - name: Retail
                - name: Business
            paths: {}
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `dashboard tags must be a list`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
              x-api-dashboard-tags: Retail
            paths: {}
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .descriptionsEqualTo("x-api-dashboard-tags must be a list of tags with a name")
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `dashboard tags are optional and top-level tags are not restricted`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            tags:
              - name: rendition service
            paths: {}
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }
}
