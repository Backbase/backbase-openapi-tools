package com.backbase.oss.boat;

import ch.qos.logback.classic.Level;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.nio.file.Files;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openapitools.codegen.serializer.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoatTerminal {

    private static final Logger log = LoggerFactory.getLogger(BoatTerminal.class);

    private static final String CLI_FILE_INPUT_OPTION = "f";
    private static final String CLI_FILE_OUTPUT_OPTION = "o";
    private static final String CLI_VERBOSE_OPTION = "v";

    @SuppressWarnings({"squid:S4823", "squid:S4792", "squid:S106"})
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(prepareOptions(), args);
            String input = commandLine.getOptionValue(CLI_FILE_INPUT_OPTION);
            String output = commandLine.getOptionValue(CLI_FILE_OUTPUT_OPTION);
            boolean hasOutputFile = commandLine.hasOption(CLI_FILE_OUTPUT_OPTION);
            boolean verbose = commandLine.hasOption(CLI_VERBOSE_OPTION);

            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

            if (verbose) {
                root.setLevel(Level.INFO);
            } else {
                root.setLevel(Level.WARN);
            }
            File inputFile = new File(input);
            if (!inputFile.exists()) {
                throw new ParseException("Input file does not exist");
            }
            OpenAPI openApi = Exporter.export(inputFile, new ExporterOptions().convertExamplesToYaml(true));

            String yaml = SerializerUtils.toYamlString(openApi);
            if (hasOutputFile) {
                File outputFile = new File(output);
                Files.write(outputFile.toPath(), yaml.getBytes());
            } else {
                System.out.println(yaml);
            }


        } catch (ParseException e) {
            log.error("Missing or unable to parse arguments.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("boat", prepareOptions());
        } catch (Exception e) {
            log.error("Failed to convert RAML to Open API: ", e);

        }
    }


    private static Options prepareOptions() {
        Options options = new Options();
        options.addRequiredOption(CLI_FILE_INPUT_OPTION, "file", true, "Input RAML 1.0 file");
        options.addOption(CLI_FILE_OUTPUT_OPTION, "output", true, "Output OpenAPI file");
        options.addOption(CLI_VERBOSE_OPTION, "verbose", false, "Verbose output");
        return options;
    }
}
