package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatViolation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.v3.OAI3Context;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

@SuppressWarnings("FieldMayBeFinal")
@Slf4j
/*
  Lint Specification
 */
public abstract class AbstractLintMojo extends InputMavenArtifactMojo {

    private static final String YAML_SUFFIX = ".yaml";
    private static final String PATH_ROOT = "$";
    private static final String PATH_SEPARATOR = ".";
    private static final String EXAMPLES = "examples";
    private static final String EXAMPLE = "example";
    private static final String SCHEMA = "schema";
    private static final String REFERENCE = "$ref";

    /**
     * Set this to <code>true</code> to fail in case a warning is found.
     */
    @Parameter(name = "failOnWarning", defaultValue = "false")
    protected boolean failOnWarning;


    /**
     * Set this to <code>true</code> to show the list of ignored rules..
     */
    @Parameter(name = "showIgnoredRules", defaultValue = "false")
    protected boolean showIgnoredRules;

    /**
     * List of rules ids which will be ignored.
     */
    @Parameter(name = "ignoreRules")
    protected String[] ignoreRules = new String[]{"150","219","215","218","166","136","174","235","107","171","224","143",
        "151","129","146","147","172","145","115","132","120", "134","183","154","105","104","130","118","110","153",
        "101","176","116","M009","H002","M010","H001","M008","S005","S006","S007","M011"};

    protected List<BoatLintReport> lint() throws MojoExecutionException, MojoFailureException {

        super.execute();

        List<BoatLintReport> boatLintReports = new ArrayList<>();

        File[] inputFiles;
        if (input.isDirectory()) {
            inputFiles = input.listFiles(pathname -> pathname.getName().endsWith(YAML_SUFFIX));
            if (inputFiles == null || inputFiles.length == 0) {
                throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
            }
            log.info("Found " + inputFiles.length + " specs to lint.");
        } else {
            inputFiles = new File[]{input};
        }

        for (File inputFile : inputFiles) {
            boatLintReports.add(lintOpenAPI(inputFile));
        }
        return boatLintReports;

    }

    /**
     * Validates schema examples across all input files.
     *
     * TODO: Consider merging with lint()
     * TODO: Make optional
     * TODO: Create appropriate report
     * TODO: Fix multiple references
     *
     */
    protected List<BoatLintReport> validateExamples() throws MojoExecutionException, MojoFailureException {
        super.execute();

        List<BoatLintReport> boatLintReports = new ArrayList<>();

        File[] inputFiles;
        if (input.isDirectory()) {
            inputFiles = input.listFiles(pathname -> pathname.getName().endsWith(YAML_SUFFIX));
            if (inputFiles == null || inputFiles.length == 0) {
                throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
            }
            log.info("Found " + inputFiles.length + " specs to validate.");
        } else {
            inputFiles = new File[]{input};
        }

        for (File inputFile : inputFiles) {
            boatLintReports.add(validateExamples(inputFile));
        }
        return boatLintReports;
    }

    private BoatLintReport lintOpenAPI(File inputFile) throws MojoExecutionException {
        try {
            if(showIgnoredRules) {
                log.info("These rules will be ignored: {}", Arrays.toString(ignoreRules));
            }
            BoatLinter boatLinter = new BoatLinter(ignoreRules);
            return boatLinter.lint(inputFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: " + inputFile, e);
        } catch (OpenAPILoaderException e) {
            throw new MojoExecutionException("Cannot load OpenAPI: " + inputFile, e);
        }
    }

    /**
     * Parses the yaml file as JSON and iterates over the nodes.
     */
    private BoatLintReport validateExamples(File inputFile) throws MojoExecutionException {
        try {
            log.info("Validating examples for: {}", inputFile);
            JsonNode rootNode = new ObjectMapper(new YAMLFactory()).readTree(inputFile);
            return validateNode(rootNode, rootNode, new BoatLintReport(), PATH_ROOT);
        } catch (IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: " + inputFile, e);
        }
    }

    /**
     * For every node, either handles the example or continues the iteration.
     *
     * @param rootNode The root node of the JSON tree
     * @param node Current node in the iteration
     * @param report A pointer to the aggregated report
     * @param currentPath A simplistic JSON path for logging purposes
     * @return the aggregated report
     */
    private BoatLintReport validateNode(JsonNode rootNode, JsonNode node, BoatLintReport report, String currentPath) {
        Iterator<Entry<String, JsonNode>> fields = node.fields();
        fields.forEachRemaining(entry -> {
            if (EXAMPLES.equals(entry.getKey())) {
                log.debug("Found \"{}\" node under {}", EXAMPLES, currentPath);
                JsonNode schemaNode = node.get(SCHEMA);
                if (schemaNode == null) {
                    log.debug("No schema node found under {}", currentPath);
                } else {
                    validateExamplesNode(rootNode, entry.getValue(), getSchema(rootNode, schemaNode), report, currentPath);
                }
            } else if (EXAMPLE.equals(entry.getKey())) {
                log.debug("Found \"{}\" node under {}", EXAMPLE, currentPath);
                JsonNode schemaNode = node.get(SCHEMA);
                if (schemaNode == null) {
                    log.debug("No schema node found under {}", currentPath);
                } else {
                    validateExampleNode(rootNode, entry.getValue(), getSchema(rootNode, schemaNode), report, currentPath);
                }
            } else {
                validateNode(rootNode, entry.getValue(), report, currentPath + PATH_SEPARATOR + entry.getKey());
            }
        });
        return report;
    }

    /**
     * Handles schema references.
     */
    private JsonNode getSchema(JsonNode rootNode, JsonNode schemaNode) {
        JsonNode schemaReference = schemaNode.get(REFERENCE);
        if (schemaReference != null) {
            String remainingPath = schemaReference.textValue().replace("\"", "");
            int pathSeparator = remainingPath.indexOf('/');
            return resolveSchemaReferences(rootNode, findSchema(rootNode, remainingPath.substring(pathSeparator + 1)));
        }
        return schemaNode;
    }

    /**
     * Resolves schema references within the example schema.
     */
    private JsonNode resolveSchemaReferences(JsonNode rootNode, JsonNode currentNode) {
//        Iterator<Entry<String, JsonNode>> fields = currentNode.fields();
//        fields.forEachRemaining(entry -> {
//            if (entry.getKey().equals(REFERENCE)) {
//
//            }
//        });
        return currentNode;
    }

    /**
     * Traverse the schema reference to the schema root node.
     */
    private JsonNode findSchema(JsonNode currentNode, String remainingPath) {
        int pathSeparator = remainingPath.indexOf('/');
        if (pathSeparator > 0) {
            String nextNode = remainingPath.substring(0, pathSeparator);
            return findSchema(currentNode.get(nextNode), remainingPath.substring(pathSeparator + 1));
        } else {
            return currentNode.get(remainingPath);
        }
    }

    /**
     * Iterates over all example instances under an "examples" node.
     */
    private void validateExamplesNode(
        JsonNode rootNode, JsonNode examplesNode, JsonNode schemaNode, BoatLintReport report, String currentPath) {

        examplesNode.fields().forEachRemaining(exampleEntry ->
            validateExampleNode(rootNode, exampleEntry.getValue(), schemaNode, report,
                currentPath + PATH_SEPARATOR + exampleEntry.getKey()));
    }

    /**
     * Validates a specific example.
     * 
     * @param exampleNode The root node for the example.
     * @param schemaNode The root node for the example schema.
     * @param report A pointer to the aggregated report.
     * @param currentPath A simplistic JSON path for logging purposes.
     */
    private void validateExampleNode(
        JsonNode rootNode, JsonNode exampleNode, JsonNode schemaNode, BoatLintReport report, String currentPath) {

        try {
//            SchemaValidator schemaValidator = new SchemaValidator(null, schemaNode);

            // Attempt at achieving reference resolution
            SchemaValidator schemaValidator = new SchemaValidator(new ValidationContext<>(
                new OAI3Context(new URL("file:/"), null, rootNode)), null, schemaNode);

            ValidationData<Void> validationData = new ValidationData<>();
            schemaValidator.validate(exampleNode, validationData);

            if (!validationData.isValid()) {
                BoatViolation violation = new BoatViolation();
                violation.setDescription(validationData.toString());
                report.getViolations().add(violation);
            }
        } catch (ResolutionException | MalformedURLException e) {
            log.error("Failed to validate node {}" + currentPath, e);
        }
    }

    @Override
    public void setInput(File input) {
        this.input = input;
    }

    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }

    public void setIgnoreRules(String[] ignoreRules) {
        this.ignoreRules = ignoreRules;
    }
}
