package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class UseWellUnderstoodStatusCodesRuleTest {

    private val cut = UseBackbaseWellUnderstoodHttpStatusCodesRule(rulesConfig)

    @Test
    fun `correct check if standard codes are used`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
              /client-api/foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    200:
                      description: Lorem Ipsum
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url
                    504:
                      description: Gateway timeout
              /service-api/bar:
                get:
                  description: Lorem Ipsum
                  operationId: bar
                  responses:
                    200:
                      description: Lorem Ipsum
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url   
                    502:
                      description: Bad Gateway
              /integration-api/bar2:
                get:
                  description: Lorem Ipsum 2
                  operationId: bar2
                  responses:
                    200:
                      description: Lorem Ipsum 2
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url 
                    422:
                      description: Unprocessable Entity
            """.trimIndent()
        )

        val violations = cut.checkWellUnderstoodResponseCodesUsage(context)

        ZallyAssertions
                .assertThat(violations)
                .isEmpty()
    }


    @Test
    fun `incorrect check standard codes are used`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
                """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
              /foo:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    202:
                      description: Lorem Ipsum
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url
                    678:
                      description: error
              /bar:
                get:
                  description: Lorem Ipsum
                  operationId: foo
                  responses:
                    202:
                      description: Lorem Ipsum
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url    
                    201:
                      description: bad
            """.trimIndent()
        )

        val violations = cut.checkWellUnderstoodResponseCodesUsage(context)

        ZallyAssertions
                .assertThat(violations)
                .isNotEmpty
    }


}