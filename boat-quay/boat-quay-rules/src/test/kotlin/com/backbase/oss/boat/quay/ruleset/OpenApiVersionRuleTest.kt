package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class OpenApiVersionRuleTest {

    private val cut = OpenApiVersionRule(rulesConfig)

    @Test
    fun `check open api version 3_0_3 return no validations`() {
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
    fun `check open api version 3_0_4 return no validations`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.4
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
    fun `check open api version 3_1_0 return no validations`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.1.0
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
    fun `check open api version 3_1_1 return no validations`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.1.1
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
    fun `check open api version 3_1_2 return no validations`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.1.2
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
    fun `check open api version 3_0_2 return violation`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.2
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
                .isNotEmpty()
    }

}