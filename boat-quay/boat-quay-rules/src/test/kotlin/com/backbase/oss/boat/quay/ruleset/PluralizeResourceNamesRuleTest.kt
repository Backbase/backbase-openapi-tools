package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory
import org.zalando.zally.core.rulesConfig

class PluralizeResourceNamesRuleTest {

    private val cut = PluralizeResourceNamesRule(rulesConfig)

    @Test
    fun `correct plural resource names`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
              /service-api/v1/portal:
                get:
                  description: Lorem Ipsum
                  operationId: bars
                  responses:
                    202:
                      description: Lorem Ipsum
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url      
              /service-api/v1/bars:
                get:
                  description: Lorem Ipsum
                  operationId: bars
                  responses:
                    202:
                      description: Lorem Ipsum
                      headers:
                        Location: # should not violate since not called `Link`
                          type: string
                          format: url      
              /integration-api/v2/bars/{barId}/beers:
                get:
                  description: Lorem Ipsum 2
                  operationId: beers
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
            .isEmpty()
    }


    @Test
    fun `incorrect plural resource names`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: Thing API
              version: 1.0.0
            paths: 
              /client-api/v1/foo:
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
              /client-api/v1/bar:
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
            .isNotEmpty
    }


}