package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory

class NoVersionInUriRuleTest {

    private val cut = NoVersionInUriRule()

    @Test
    fun `correct path prefix without version`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
              x-api-type: Unified Backbase API
            paths: 
              /api/foo:
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
              /api/bar:
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
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `incorrect path prefix with version`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
              x-api-type: Unified Backbase API
            paths: 
              /api/foo/v1true:
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
              /api/v1/bar:
                get:
                  description: Lorem Ipsum
                  operationId: bar
                  responses:
                    202:
                      description: Lorem Ipsum
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url      
              /v2/bar2:
                get:
                  description: Lorem Ipsum 2
                  operationId: bar2
                  responses:
                    202:
                      description: Lorem Ipsum 2
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url                                      
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isNotEmpty
    }

}