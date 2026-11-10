package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class NoVerbsInPathRuleTest {

    private val cut = NoVerbsInPathRule(rulesConfig)

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `verbs in paths are reported`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/things/{thingId}/update:
                post:
                  responses:
                    '200':
                      description: ok
              /client-api/v1/create-thing:
                post:
                  responses:
                    '201':
                      description: created
              /things/searchThings:
                post:
                  responses:
                    '200':
                      description: ok
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo(
                "Path segment 'update' is a verb, use the HTTP method instead",
                "Path segment 'create-thing' is a verb, use the HTTP method instead",
                "Path segment 'searchThings' is a verb, use the HTTP method instead"
            )
            .pointersEqualTo(
                "/paths/~1client-api~1v1~1things~1{thingId}~1update",
                "/paths/~1client-api~1v1~1create-thing",
                "/paths/~1things~1searchThings"
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `nouns, path variables and webhook names are allowed`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 1.0.0
            paths:
              /client-api/v1/addresses/{getId}:
                get:
                  responses:
                    '200':
                      description: ok
              /service-api/v1/updates:
                get:
                  responses:
                    '200':
                      description: ok
              /service-api/v1/listings/settings:
                get:
                  responses:
                    '200':
                      description: ok
              /integration-api/v1/webhooks/process-payment:
                post:
                  responses:
                    '204':
                      description: no content
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
