package com.backbase.oss.boat.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import com.backbase.oss.boat.diff.model.ChangedOpenApi;
import com.backbase.oss.boat.diff.model.ChangedOpenApiRenderList;
import com.backbase.oss.boat.diff.output.ConsoleRender;
import com.backbase.oss.boat.diff.output.HtmlRender;
import com.backbase.oss.boat.diff.output.MarkdownRender;
import com.backbase.oss.boat.diff.output.SumJsonRender;
import lombok.SneakyThrows;
import org.slf4j.Logger;

public class TestUtils {

    public static final Logger LOG = getLogger(TestUtils.class);

    public static void assertOpenApiAreEquals(String oldSpec, String newSpec) {
        ChangedOpenApi changedOpenApi = OpenApiCompare.fromLocations(oldSpec, newSpec);
        LOG.info("Result: {}", changedOpenApi.isChanged().getValue());
        assertThat(changedOpenApi.getNewEndpoints()).isEmpty();
        assertThat(changedOpenApi.getMissingEndpoints()).isEmpty();
        assertThat(changedOpenApi.getChangedOperations()).isEmpty();
        renderEverything(changedOpenApi);
    }

    public static void assertOpenApiChangedEndpoints(String oldSpec, String newSpec) {
        ChangedOpenApi changedOpenApi = OpenApiCompare.fromLocations(oldSpec, newSpec);
        LOG.info("Result: {}", changedOpenApi.isChanged().getValue());
        assertThat(changedOpenApi.getNewEndpoints()).isEmpty();
        assertThat(changedOpenApi.getMissingEndpoints()).isEmpty();
        assertThat(changedOpenApi.getChangedOperations()).isNotEmpty();
    }

    public static void assertOpenApiBackwardCompatible(
        String oldSpec, String newSpec, boolean isDiff) {
        ChangedOpenApi changedOpenApi = OpenApiCompare.fromLocations(oldSpec, newSpec);
        LOG.info("Result: {}", changedOpenApi.isChanged().getValue());
        assertThat(changedOpenApi.isCompatible()).isTrue();
        renderEverything(changedOpenApi);
    }

    public static void assertOpenApiBackwardIncompatible(String oldSpec, String newSpec) {
        ChangedOpenApi changedOpenApi = OpenApiCompare.fromLocations(oldSpec, newSpec);
        LOG.info("Result: {}", changedOpenApi.isChanged().getValue());
        assertThat(changedOpenApi.isIncompatible()).isTrue();
        renderEverything(changedOpenApi);
    }

    @SneakyThrows
    public static void renderEverything(ChangedOpenApi changedOpenApi) {
        if (changedOpenApi != null) {
            new ConsoleRender().render(changedOpenApi);
            new MarkdownRender().render(changedOpenApi);
            new HtmlRender().render(changedOpenApi);
            new SumJsonRender().render(new ChangedOpenApiRenderList(changedOpenApi));
        }

    }
}
