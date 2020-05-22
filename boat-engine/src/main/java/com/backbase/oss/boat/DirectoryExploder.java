package com.backbase.oss.boat;

import com.backbase.oss.boat.example.NamedExample;
import com.backbase.oss.boat.transformers.OpenAPIExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.CaseFormat;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class DirectoryExploder {

    private static final Logger log = LoggerFactory.getLogger(DirectoryExploder.class);

    @NotNull
    private final OpenAPIExtractor openAPIExtractor;

    public DirectoryExploder(@NotNull OpenAPIExtractor openAPIExtractor) {
        this.openAPIExtractor = openAPIExtractor;
    }

    public void serializeIntoDirectory(@NotNull String outputDir) throws IOException {
        List<NamedExample> examples = openAPIExtractor.extractExamples();
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter()); // todo - extract as field
        Path outputPath = Paths.get(outputDir);
        if (Files.notExists(outputPath)) {
            Files.createDirectories(outputPath);
        } else {
            FileUtils.cleanDirectory(outputPath.toFile());
        }
        examples.forEach(namedExample -> {
            String title = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, namedExample.getName());
            String serializedValue = "";
            try {
                serializedValue = writer.writeValueAsString(namedExample.getExample());
                Path exampleFile = outputPath.resolve(title + ".json");
                if (Files.notExists(exampleFile)) {
                    Files.createFile(exampleFile);
                }
                Files.write(exampleFile, serializedValue.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            } catch (JsonProcessingException e) {
                log.error("Could not serialize to JSON", e);
            } catch (IOException e) {
                log.error("Could not write to file: {}.\nError: {}.\nSerialized value: {}", title, e, serializedValue);
            }
        });
    }
}