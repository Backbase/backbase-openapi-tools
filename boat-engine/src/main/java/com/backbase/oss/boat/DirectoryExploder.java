package com.backbase.oss.boat;

import com.backbase.oss.boat.example.NamedExample;
import com.backbase.oss.boat.transformers.OpenAPIExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.CaseFormat;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class DirectoryExploder {

    private static final Logger log = LoggerFactory.getLogger(DirectoryExploder.class);

    private static final String EXAMPLES_DIR = "examples";

    @NotNull
    private final OpenAPIExtractor openAPIExtractor;
    @NotNull
    private final ObjectWriter writer;

    public DirectoryExploder(@NotNull OpenAPIExtractor openAPIExtractor, @NotNull ObjectWriter writer) {
        this.openAPIExtractor = openAPIExtractor;
        this.writer = writer;
    }

    public void serializeIntoDirectory(@NotNull String outputDir) throws IOException {
        List<NamedExample> examples = openAPIExtractor.extractInlineExamples();
        Path outputPath = Paths.get(outputDir);
        if (Files.notExists(outputPath)) {
            Path examplesPath = Paths.get(outputDir, EXAMPLES_DIR);
            Files.createDirectories(examplesPath);
        } else {
            FileUtils.cleanDirectory(outputPath.toFile());
        }
        examples.forEach(namedExample -> {
            String title = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, namedExample.getName());
            String serializedValue = "";
            try {
                serializedValue = writer.writeValueAsString(namedExample.getExample().getValue());
                String filename = title + ".json";
                Path exampleFile = outputPath.resolve(filename);
                if (Files.notExists(exampleFile)) {
                    Files.createFile(exampleFile);
                }
                Files.write(exampleFile, serializedValue.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                namedExample.getExample().setValue(null);
                String $ref = "'" + EXAMPLES_DIR + File.separator + filename + "'";
                namedExample.getExample().set$ref($ref);
            } catch (JsonProcessingException e) {
                log.error("Could not serialize to JSON", e);
            } catch (IOException e) {
                log.error("Could not write to file: {}.\nError: {}.\nSerialized value: {}", title, e, serializedValue);
            }
        });
    }
}