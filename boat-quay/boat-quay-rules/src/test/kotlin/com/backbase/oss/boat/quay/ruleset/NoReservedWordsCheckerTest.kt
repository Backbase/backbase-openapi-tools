package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class NoReservedWordsCheckerTest {

    private val cut = NoReservedWordsChecker(rulesConfig)

    @Test
    fun `check no reserved words`() {
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
                OtherThing:
                  type: object
                  properties:
                    theOtherNumber:
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
    fun `check spring reserved words fail`() {
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
                    public:
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

    @Test
    fun `check js reserved words fail`() {
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
                    function:
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

    //    https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/swift5.md#reserved-words
    @Test
    fun `check swift reserved words fail`() {
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
                    unowned:
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

    //    https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/kotlin.md#reserved-words
    @Test
    fun `check kotlin reserved words fail`() {
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
                    fun:
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
