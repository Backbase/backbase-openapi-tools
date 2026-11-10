package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.zalando.zally.core.DefaultContextFactory

class PathVersionMatchesMajorRuleTest {

    private val cut = PathVersionMatchesMajorRule()

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `path version that differs from the major version is reported`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: 2.3.0
            paths:
              /client-api/v2/things:
                get:
                  responses:
                    '200':
                      description: ok
              /client-api/v1/things/{thingId}:
                get:
                  responses:
                    '200':
                      description: ok
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .descriptionsEqualTo("Path version v1 does not match major version 2 of info.version 2.3.0")
            .pointersEqualTo("/paths/~1client-api~1v1~1things~1{thingId}")
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.0.3", "3.1.0"])
    fun `matching and unversioned paths are allowed`(openApiVersion: String) {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: $openApiVersion
            info:
              title: Thing API
              version: v1.4.2
            paths:
              /client-api/v1/things:
                get:
                  responses:
                    '200':
                      description: ok
              /things/{thingId}:
                get:
                  responses:
                    '200':
                      description: ok
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
