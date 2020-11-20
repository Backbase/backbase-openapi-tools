package com.backbase.oss.boat;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.OpenAPIExtractor;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import io.swagger.v3.oas.models.OpenAPI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = ExportCommand.NAME,
    description = "Converts a RAML spec to an OpenAPI spec.",
    mixinStandardHelpOptions = true)
public class ExportCommand implements Callable<Integer> {
    static public final String NAME = "export";

    @Parameters(description = "Input RAML 1.0 file.", converter = ExistingPath.class)
    private Path input;

    @Option(names = {"-o", "--output"}, order = 20,
        description = "Output OpenAPI file name.")
    private Path output;

    @Option(names = {"-d", "--directory"}, order = 30,
        description = "Output OpenAPI directory.")
    private Path directory;

    @Option(names = {"--convert-examples"}, order = 40,
        description = "Convert examples to YAML.",
        defaultValue = "true",
        arity = "0..1",
        paramLabel = "true|false",
        showDefaultValue = Visibility.ALWAYS, fallbackValue = "true")
    private boolean convertExamples;

    @Override
    public Integer call() throws Exception {
        final ExporterOptions options = new ExporterOptions().convertExamplesToYaml(this.convertExamples);
        final OpenAPI openApi = Exporter.export(this.input.toFile(), options);
        final String yaml = SerializerUtils.toYamlString(openApi);

        if (this.directory != null) {
            final OpenAPIExtractor extractor = new OpenAPIExtractor(openApi);
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

            new DirectoryExploder(extractor, writer)
                .serializeIntoDirectory(this.directory);

            SerializerUtils.write(this.directory.resolve("openapi.yaml"), yaml);
        }
        if (this.output != null) {
            SerializerUtils.write(this.output, yaml);
        }
        if (this.output == null && this.directory == null) {
            System.out.print(yaml);
        }

        return 0;
    }
}

