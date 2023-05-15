package com.backbase.oss.boat;

import com.backbase.oss.boat.example.NamedExample;
import com.backbase.oss.boat.transformers.OpenAPIExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.CaseFormat;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Explodes inline elements (currently only examples) into the output directory's examples/ subdirectory, as json files.
 */
public class DirectoryExploder {

    private static final Logger log = LoggerFactory.getLogger(DirectoryExploder.class);

    private static final String EXAMPLES_DIR = "examples";

    @NotNull
    private final OpenAPIExtractor openAPIExtractor;
    @NotNull
    private final ObjectWriter writer;

    /**
     * Constructs a new instance of a DirectoryExploder.
     *
     * @param openAPIExtractor extracts inline elements from the obtained OpenAPI spec.
     * @param writer           writes the elements into files, with the configured format.
     */
    public DirectoryExploder(@NotNull OpenAPIExtractor openAPIExtractor, @NotNull ObjectWriter writer) {
        this.openAPIExtractor = openAPIExtractor;
        this.writer = writer;
    }

    /**
     * Extracts inline examples from the spec by means of {@link OpenAPIExtractor#extractInlineExamples()}.
     * Uses the extractor passed to the constructor.
     * Writes the examples into the examples/ directory of {@code outputDir}.
     * Cleans any existing content inside {@code outputDir} if present, but not that directory itself.
     *
     * @param outputPath the directory which will contain the exploded examples in the examples/ subdirectory.
     *                  Can contain multiple directories, as in my-dir1/my-dir2/my-dir3.
     *                  Using the correct file separator (/ or \) for the current file system is the responsibility of the caller.
     * @throws IOException if the existing content of the {@code outputDir} cannot be deleted, or if {@code outputPath} cannot be created.
     */
    public void serializeIntoDirectory(@NotNull Path outputPath) throws IOException {
        List<NamedExample> examples = openAPIExtractor.extractInlineExamples();
        if (Files.exists(outputPath)) {
            FileUtils.cleanDirectory(outputPath.toFile());
        }
        Path examplesPath = outputPath.resolve(EXAMPLES_DIR);
        Files.createDirectories(examplesPath);
        examples.forEach(namedExample -> {
            String title = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, namedExample.getName());
            String serializedValue = "";
            try {
                serializedValue = writer.writeValueAsString(namedExample.getExample().getValue());
                String filename = title + ".json";
                Path exampleFile = examplesPath.resolve(filename);
                if (Files.notExists(exampleFile)) {
                    Files.createFile(exampleFile);
                }
                Files.write(exampleFile, serializedValue.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
                namedExample.getExample().setValue(null);
                String ref = EXAMPLES_DIR + File.separator + filename;
                namedExample.getExample().set$ref(ref);
            } catch (JsonProcessingException e) {
                log.error("Could not serialize to JSON", e);
            } catch (IOException e) {
                log.error("Could not write to file: {}.\nError: {}.\nSerialized value: {}", title, e, serializedValue);
            }
        });
    }
}