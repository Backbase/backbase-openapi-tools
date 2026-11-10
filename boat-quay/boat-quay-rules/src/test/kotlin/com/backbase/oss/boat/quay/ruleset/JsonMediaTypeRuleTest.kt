package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory

class JsonMediaTypeRuleTest {

    private val cut = JsonMediaTypeRule()

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `application json with parameters is reported`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things:
                post:
                  requestBody:
                    content:
                      application/json;charset=UTF-8:
                        schema:
                          type: object
                  responses:
                    '201':
                      description: created
                      content:
                        application/json; charset=utf-8:
                          schema:
                            type: object
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "Use application/json instead of application/json;charset=UTF-8",
                "Use application/json instead of application/json; charset=utf-8"
            )
            .pointersEqualTo(
                "/paths/~1client-api~1v1~1things/post/requestBody/content/application~1json;charset=UTF-8",
                "/paths/~1client-api~1v1~1things/post/responses/201/content/application~1json; charset=utf-8"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `application json and other media types are allowed`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things:
                post:
                  requestBody:
                    content:
                      application/json:
                        schema:
                          type: object
                  responses:
                    '201':
                      description: created
                      content:
                        application/json:
                          schema:
                            type: object
                    '400':
                      description: bad request
                      content:
                        application/problem+json:
                          schema:
                            type: object
              /client-api/v1/things/export:
                get:
                  responses:
                    '200':
                      description: ok
                      content:
                        text/csv; charset=utf-8:
                          schema:
                            type: string
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
