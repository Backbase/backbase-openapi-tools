package com.backbase.oss.boat;

import com.backbase.oss.boat.transformers.OpenAPIExtractor;

import javax.validation.constraints.NotNull;
import java.io.File;

public class DirectoryExploder {

    @NotNull
    private final OpenAPIExtractor openAPIExtractor;

    public DirectoryExploder(@NotNull OpenAPIExtractor openAPIExtractor) {
        this.openAPIExtractor = openAPIExtractor;
    }

    public void serializeIntoDirectory(@NotNull File outputDir) {
    }
}