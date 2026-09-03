package com.backbase.oss.boat.quay.ruleset

import com.backbase.oss.boat.quay.ruleset.test.ZallyAssertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.zalando.zally.core.DefaultContextFactory

class InfoBlockSunsetDateCheckerTest {

    private val cut = InfoBlockSunsetDateChecker()

    @Test
    fun `x-deprecated true with valid x-sunset-date passes`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
              x-deprecated: true
              x-sunset-date: "2027-04-15"
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `x-deprecated true without x-sunset-date fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
              x-deprecated: true
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isNotEmpty
    }

    @Test
    fun `x-deprecated true with empty x-sunset-date fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
              x-deprecated: true
              x-sunset-date: ""
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isNotEmpty
    }

    @Test
    fun `x-deprecated true with malformed x-sunset-date fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
              x-deprecated: true
              x-sunset-date: "2027.04"
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isNotEmpty
    }

    @Test
    fun `x-deprecated false without x-sunset-date passes`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
              x-deprecated: false
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `no x-deprecated without x-sunset-date passes`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }

    @Test
    fun `x-deprecated false with x-sunset-date fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
              x-deprecated: false
              x-sunset-date: "2027-04-15"
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isNotEmpty
    }

    @Test
    fun `no x-deprecated with x-sunset-date fails`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
              x-sunset-date: "2027-04-15"
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isNotEmpty
    }

    @Test
    fun `no extensions passes`() {
        @Language("YAML")
        val context = DefaultContextFactory().getOpenApiContext(
            """
            openapi: 3.0.3
            info:
              title: API
              version: 1.0.0
            """.trimIndent()
        )

        val violations = cut.validate(context)

        ZallyAssertions
            .assertThat(violations)
            .isEmpty()
    }
}
