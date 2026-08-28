package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class OpenApiVersionRuleTest {

    private val cut = OpenApiVersionRule(rulesConfig)

    /**
     * Builds a context for a given OpenAPI version. Only versions swagger-parser can actually represent
     * (3.0.x and 3.1.x) can be used here: for anything else the parser yields no document at all, so the
     * spec never reaches this rule.
     */
    private fun contextFor(version: String) = DefaultContextFactory().getOpenApiContext(
            """
        openapi: $version
        info:
          title: Thing API
          version: 1.0.0
        components:
          schemas:
            Thing:
              type: object
              properties:
                theNumber:
                  type: integer
                  format: int32
                  minimum: 0
                  maximum: 10
        """.trimIndent()
    )

    @Test
    fun `check open api version return no validations`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            components:
              schemas:
                Thing:
                  type: object
                  properties:
                    theNumber:
                      type: integer
                      format: int32
                      minimum: 0
                      maximum: 10
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }

    @Test
    fun `accepts every explicitly allowed 3 0 version`() {
        listOf("3.0.3", "3.0.4").forEach { version ->
            ZallyAssertions.assertThat(cut.validate(contextFor(version))).isEmpty()
        }
    }

    @Test
    fun `accepts any 3 1 patch version`() {
        listOf("3.1.0", "3.1.1", "3.1.2").forEach { version ->
            ZallyAssertions.assertThat(cut.validate(contextFor(version))).isEmpty()
        }
    }

    @Test
    fun `reports a violation for a 3 0 version that is not allowed`() {
        listOf("3.0.0", "3.0.1", "3.0.2").forEach { version ->
            ZallyAssertions
                    .assertThat(cut.validate(contextFor(version)))
                    .pointersEqualTo("/openapi")
        }
    }
}
