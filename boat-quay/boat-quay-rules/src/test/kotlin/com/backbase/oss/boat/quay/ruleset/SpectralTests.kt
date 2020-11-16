package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.ruleset.zalando.JsonProblemAsDefaultResponseRule
import org.zalando.zally.ruleset.zalando.SuccessResponseAsJsonObjectRule

class SpectralTests {

    private val cut = NoLicenseAllowedChecker()

    @Test
    fun `operation-2xx-response`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /path:
                get:
                  responses:
                    418:
                      description: teapot
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

        val violations = JsonProblemAsDefaultResponseRule().checkContainsDefaultResponse(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }



}