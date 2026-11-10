package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class ForbiddenResponseCodesRuleTest {

    private val cut = ForbiddenResponseCodesRule(rulesConfig)

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `405, 408, 422 and 5xx responses are reported`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things:
                get:
                  responses:
                    '200':
                      description: ok
                    '405':
                      description: method not allowed
                    '408':
                      description: request timeout
                    '422':
                      description: unprocessable entity
                    '500':
                      description: internal server error
                    5XX:
                      description: server error
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "405 response should not be defined",
                "408 response should not be defined",
                "422 response should not be defined",
                "500 response should not be defined",
                "5XX response should not be defined"
            )
            .pointersEqualTo(
                "/paths/~1client-api~1v1~1things/get/responses/405",
                "/paths/~1client-api~1v1~1things/get/responses/408",
                "/paths/~1client-api~1v1~1things/get/responses/422",
                "/paths/~1client-api~1v1~1things/get/responses/500",
                "/paths/~1client-api~1v1~1things/get/responses/5XX"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `other responses are allowed`(openApiVersion: String) {
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
                  responses:
                    '201':
                      description: created
                    '400':
                      description: bad request
                    '401':
                      description: unauthorized
                    '403':
                      description: forbidden
                    '409':
                      description: conflict
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
