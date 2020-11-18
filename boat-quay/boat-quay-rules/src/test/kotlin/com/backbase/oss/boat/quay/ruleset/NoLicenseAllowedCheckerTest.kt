package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.zalando.zally.core.DefaultContextFactory

class NoLicenseAllowedCheckerTest {

    private val cut = NoLicenseAllowedChecker()

    @Test
    fun `check open api version has no license`() {
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
    fun `check open api version has a license`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
              license: 
                description: "My License" 
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
                .isNotEmpty
    }


}