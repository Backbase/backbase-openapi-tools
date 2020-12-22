package com.backbase.oss.boat;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import ch.qos.logback.classic.Level;
import com.backbase.oss.boat.BoatTerminal.VersionProvider;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.OpenAPIExtractor;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "boat-terminal",
    description = "Boat Terminal",
    versionProvider = VersionProvider.class,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    sortOptions = false)
@Slf4j
public class BoatTerminal implements Runnable {

    static class VersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            return new String[] {getClass().getPackage().getImplementationVersion()};
        }
    }

    static class Input {
        @Option(names = {"-f", "--file"}, paramLabel = "<input>",
            description = "Input RAML 1.0 file (deprecated, use the input as a parameter).")
        private File inputOpt;

        @Parameters(description = "Input RAML 1.0 file.")
        private File inputFile;

        File get() {
            return this.inputFile != null ? this.inputFile : this.inputOpt;
        }
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        return new CommandLine(new BoatTerminal())
            .addSubcommand("completion", new GenerateCompletion())
            .execute(args);
    }

    private final Logger root;

    public Level getRootLevel() {
        return root.getLevel();
    }

    public BoatTerminal() {
        this.root = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);

        this.root.setLevel(ch.qos.logback.classic.Level.WARN);
    }

    @ArgGroup(exclusive = true, multiplicity = "1", order = 10)
    private Input input;

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

    @Option(names = {"-v", "--verbose"}, order = 50,
        description = "Verbose output; multiple -v options increase the verbosity.")
    public void setVerbose(boolean[] verbose) {
        switch (verbose.length) {
            case 1:
                this.root.setLevel(ch.qos.logback.classic.Level.INFO);
                break;

            case 2:
                this.root.setLevel(ch.qos.logback.classic.Level.DEBUG);
                break;

            default:
                this.root.setLevel(ch.qos.logback.classic.Level.TRACE);
                break;
        }
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (final Exception e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            } else {
                log.error(e.getMessage());
            }
        }
    }

    private void execute() throws ExportException, IOException {
        final File inputFile = this.input.get();

        if (!inputFile.exists()) {
            throw new FileNotFoundException("Not found: " + inputFile.getAbsolutePath());
        }

        final ExporterOptions options = new ExporterOptions().convertExamplesToYaml(this.convertExamples);
        final OpenAPI openApi = Exporter.export(inputFile, options);
        final String yaml = SerializerUtils.toYamlString(openApi);

        if (this.directory != null) {
            final OpenAPIExtractor extractor = new OpenAPIExtractor(openApi);
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

            new DirectoryExploder(extractor, writer)
                .serializeIntoDirectory(this.directory);

            Files.write(this.directory.resolve("openapi.yaml"),
                yaml.getBytes(StandardCharsets.UTF_8));
        }
        if (this.output != null) {
            final Path parent = this.output.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(this.output,
                yaml.getBytes(StandardCharsets.UTF_8));
        }
        if (this.output == null && this.directory == null) {
            log.debug("Output path for {}, is null", yaml);
        }
    }
}
