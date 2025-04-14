package com.backbase.oss.boat;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyAdditionalPropertiesKvp;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyAdditionalPropertiesKvpList;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyImportMappingsKvp;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyImportMappingsKvpList;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyInstantiationTypesKvp;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyInstantiationTypesKvpList;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyLanguageSpecificPrimitivesCsv;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyLanguageSpecificPrimitivesCsvList;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyReservedWordsMappingsKvp;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyReservedWordsMappingsKvpList;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applySchemaMappingsKvp;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applySchemaMappingsKvpList;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyServerVariablesKvp;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyServerVariablesKvpList;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyTypeMappingsKvp;
import static org.openapitools.codegen.config.CodegenConfiguratorUtils.applyTypeMappingsKvpList;

import com.backbase.oss.boat.transformers.Bundler;
import com.backbase.oss.boat.transformers.DereferenceComponentsPropertiesTransformer;
import com.backbase.oss.boat.transformers.UnAliasTransformer;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.util.ClasspathHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.auth.AuthParser;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.utils.OptionUtils;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

/**
 * Generates client/server code from an OpenAPI json/yaml definition.
 */
@SuppressWarnings({"DefaultAnnotationParam", "java:S3776", "java:S5411"})
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
@Slf4j
public class GenerateMojo extends InputMavenArtifactMojo {

    private static String trimCSV(String text) {
        if (isNotEmpty(text)) {
            return stream(text.split("[,;]+"))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(joining(","));
        } else {
            return "";
        }
    }

    static String uniqueJoin(Collection<String> values) {
        return values.stream()
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .distinct()
            .sorted()
            .collect(Collectors.joining(","));
    }

    public static final String INSTANTIATION_TYPES = "instantiation-types";
    public static final String IMPORT_MAPPINGS = "import-mappings";
    public static final String TYPE_MAPPINGS = "type-mappings";
    public static final String LANGUAGE_SPECIFIC_PRIMITIVES = "language-specific-primitives";
    public static final String ADDITIONAL_PROPERTIES = "additional-properties";
    public static final String SERVER_VARIABLES = "server-variables";
    public static final String RESERVED_WORDS_MAPPINGS = "reserved-words-mappings";

    public static final String SCHEMA_MAPPING = "schema-mappings";

    /**
     * The build context is only avail when running from within eclipse. It is used to update the
     * eclipse-m2e-layer when the plugin is executed inside the IDE.
     */
    @Component
    protected BuildContext buildContext = new DefaultBuildContext();

    @Parameter(name = "verbose", required = false, defaultValue = "false")
    protected boolean verbose;

    /**
     * Client language to generate.
     */
    @Parameter(name = "language")
    protected String language;


    /**
     * The name of the generator to use.
     */
    @Parameter(name = "generatorName", property = "openapi.generator.maven.plugin.generatorName")
    protected String generatorName;

    /**
     * Location of the output directory.
     */
    @Parameter(name = "output", property = "openapi.generator.maven.plugin.output",
        defaultValue = "${project.build.directory}/generated-sources/openapi")
    protected File output;


    /**
     * Copy input spec file to location.
     */
    @Parameter(name = "copyTo", defaultValue = "${project.build.outputDirectory}/META-INF/openapi/openapi.yaml")
    protected File copyTo;


    /**
     * Git host, e.g. gitlab.com.
     */
    @Parameter(name = "gitHost", property = "openapi.generator.maven.plugin.gitHost", required = false)
    protected String gitHost;

    /**
     * Git user ID, e.g. swagger-api.
     */
    @Parameter(name = "gitUserId", property = "openapi.generator.maven.plugin.gitUserId", required = false)
    protected String gitUserId;

    /**
     * Git repo ID, e.g. openapi-generator.
     */
    @Parameter(name = "gitRepoId", property = "openapi.generator.maven.plugin.gitRepoId", required = false)
    protected String gitRepoId;

    /**
     * Folder containing the template files.
     */
    @Parameter(name = "templateDirectory", property = "openapi.generator.maven.plugin.templateDirectory")
    protected File templateDirectory;

    /**
     * The name of templating engine to use, "mustache" (default) or "handlebars" (beta).
     */
    @Parameter(name = "engine", defaultValue = "mustache", property = "openapi.generator.maven.plugin.engine")
    protected String engine;

    /**
     * Adds authorization headers when fetching the swagger definitions remotely. " Pass in a
     * URL-encoded string of name:header with a comma separating multiple values
     */
    @Parameter(name = "auth", property = "openapi.generator.maven.plugin.auth")
    protected String auth;

    /**
     * Path to separate json configuration file.
     */
    @Parameter(name = "configurationFile", property = "openapi.generator.maven.plugin.configurationFile", required = false)
    protected String configurationFile;

    /**
     * Specifies if the existing files should be overwritten during the generation.
     */
    @Parameter(name = "skipOverwrite", property = "openapi.generator.maven.plugin.skipOverwrite", required = false)
    protected Boolean skipOverwrite;

    /**
     * The package to use for generated api objects/classes.
     */
    @Parameter(name = "apiPackage", property = "openapi.generator.maven.plugin.apiPackage")
    protected String apiPackage;

    /**
     * The package to use for generated model objects/classes.
     */
    @Parameter(name = "modelPackage", property = "openapi.generator.maven.plugin.modelPackage")
    protected String modelPackage;

    /**
     * The package to use for the generated invoker objects.
     */
    @Parameter(name = "invokerPackage", property = "openapi.generator.maven.plugin.invokerPackage")
    protected String invokerPackage;

    /**
     * The default package to use for the generated objects.
     */
    @Parameter(name = "packageName", property = "openapi.generator.maven.plugin.packageName")
    protected String packageName;

    /**
     * groupId in generated pom.xml.
     */
    @Parameter(name = "groupId", property = "openapi.generator.maven.plugin.groupId")
    protected String groupId;

    /**
     * artifactId in generated pom.xml.
     */
    @Parameter(name = "artifactId", property = "openapi.generator.maven.plugin.artifactId")
    protected String artifactId;

    /**
     * artifact version in generated pom.xml.
     */
    @Parameter(name = "artifactVersion", property = "openapi.generator.maven.plugin.artifactVersion")
    protected String artifactVersion;

    /**
     * Sets the library.
     */
    @Parameter(name = "library", property = "openapi.generator.maven.plugin.library", required = false)
    protected String library;

    /**
     * Sets the suffix for API classes.
     */
    @Parameter(name = "apiNameSuffix", property = "openapi.generator.maven.plugin.apiNameSuffix", required = false)
    protected String apiNameSuffix;

    /**
     * Sets the prefix for model enums and classes.
     */
    @Parameter(name = "modelNamePrefix", property = "openapi.generator.maven.plugin.modelNamePrefix", required = false)
    protected String modelNamePrefix;

    /**
     * Sets the suffix for model enums and classes.
     */
    @Parameter(name = "modelNameSuffix", property = "openapi.generator.maven.plugin.modelNameSuffix", required = false)
    protected String modelNameSuffix;

    /**
     * Sets an optional ignoreFileOverride path.
     */
    @Parameter(name = "ignoreFileOverride", property = "openapi.generator.maven.plugin.ignoreFileOverride", required = false)
    protected String ignoreFileOverride;

    /**
     * Sets custom User-Agent header value.
     */
    @Parameter(name = "httpUserAgent", property = "openapi.generator.maven.plugin.httpUserAgent", required = false,
        defaultValue = "${project.artifactId}-${project.version}")
    protected String httpUserAgent;

    /**
     * To remove operationId prefix (e.g. user_getName =&gt; getName).
     */
    @Parameter(name = "removeOperationIdPrefix", property = "openapi.generator.maven.plugin.removeOperationIdPrefix", required = false)
    protected Boolean removeOperationIdPrefix;

    /**
     * To write all log messages (not just errors) to STDOUT.
     */
    @Parameter(name = "logToStderr", property = "openapi.generator.maven.plugin.logToStderr", required = false)
    protected Boolean logToStderr;

    /**
     * To file post-processing hook.
     */
    @Parameter(name = "enablePostProcessFile", property = "openapi.generator.maven.plugin.enablePostProcessFile", required = false)
    protected Boolean enablePostProcessFile;

    /**
     * To skip spec validation.
     */
    @Parameter(name = "skipValidateSpec", property = "openapi.generator.maven.plugin.skipValidateSpec", required = false)
    protected Boolean skipValidateSpec;

    /**
     * To treat a document strictly against the spec.
     */
    @Parameter(name = "strictSpec", property = "openapi.generator.maven.plugin.strictSpec", required = false)
    protected Boolean strictSpec;

    /**
     * To generate alias (array, map) as model.
     */
    @Parameter(name = "generateAliasAsModel", property = "openapi.generator.maven.plugin.generateAliasAsModel", required = false)
    protected Boolean generateAliasAsModel;

    /**
     * A map of language-specific parameters as passed with the -c option to the command line.
     */
    @Parameter(name = "configOptions")
    protected Map<?, ?> configOptions;

    /**
     * A map of types and the types they should be instantiated as.
     */
    @Parameter(name = "instantiationTypes", property = "openapi.generator.maven.plugin.instantiationTypes")
    protected List<String> instantiationTypes;

    /**
     * A map of classes and the import that should be used for that class.
     */
    @Parameter(name = "importMappings", property = "openapi.generator.maven.plugin.importMappings")
    protected List<String> importMappings;

    /**
     * A map of swagger spec types and the generated code types to use for them.
     */
    @Parameter(name = "typeMappings", property = "openapi.generator.maven.plugin.typeMappings")
    protected List<String> typeMappings;

    /**
     * A map of additional language specific primitive types.
     */
    @Parameter(name = "languageSpecificPrimitives", property = "openapi.generator.maven.plugin.languageSpecificPrimitives")
    protected List<String> languageSpecificPrimitives;

    /**
     * A map of additional properties that can be referenced by the mustache templates.
     */
    @Parameter(name = "additionalProperties", property = "openapi.generator.maven.plugin.additionalProperties")
    protected List<String> additionalProperties;

    /**
     * A map of server variable overrides for specs that support server URL templating.
     */
    @Parameter(name = "serverVariableOverrides", property = "openapi.generator.maven.plugin.serverVariableOverrides")
    protected List<String> serverVariableOverrides;

    /**
     * A map of reserved names and how they should be escaped.
     */
    @Parameter(name = "reservedWordsMappings", property = "openapi.generator.maven.plugin.reservedWordMappings")
    protected List<String> reservedWordsMappings;

    /**
     * Generate the apis.
     */
    @Parameter(name = "generateApis", property = "openapi.generator.maven.plugin.generateApis", required = false)
    protected Boolean generateApis = true;

    /**
     * Generate the models.
     */
    @Parameter(name = "generateModels", property = "openapi.generator.maven.plugin.generateModels", required = false)
    protected Boolean generateModels = true;

    /**
     * A comma separated list of models to generate. All models is the default.
     */
    @Parameter(name = "modelsToGenerate", property = "openapi.generator.maven.plugin.modelsToGenerate", required = false)
    protected String modelsToGenerate = "";

    /**
     * A comma separated list of apis to generate. All apis is the default.
     */
    @Parameter(name = "apisToGenerate", property = "openapi.generator.maven.plugin.apisToGenerate", required = false)
    protected String apisToGenerate = "";

    /**
     * Generate the supporting files.
     */
    @Parameter(name = "generateSupportingFiles", property = "openapi.generator.maven.plugin.generateSupportingFiles", required = false)
    protected Boolean generateSupportingFiles = true;

    /**
     * A comma separated list of models to generate. All models is the default.
     */
    @Parameter(name = "supportingFilesToGenerate", property = "openapi.generator.maven.plugin.supportingFilesToGenerate", required = false)
    protected String supportingFilesToGenerate = "";

    /**
     * Generate the model tests.
     */
    @Parameter(name = "generateModelTests", property = "openapi.generator.maven.plugin.generateModelTests", required = false)
    protected Boolean generateModelTests = true;

    /**
     * Generate the model documentation.
     */
    @Parameter(name = "generateModelDocumentation", property = "openapi.generator.maven.plugin.generateModelDocumentation", required = false)
    protected Boolean generateModelDocumentation = true;

    /**
     * Generate the api tests.
     */
    @Parameter(name = "generateApiTests", property = "openapi.generator.maven.plugin.generateApiTests", required = false)
    protected Boolean generateApiTests = true;

    /**
     * Generate the api documentation.
     */
    @Parameter(name = "generateApiDocumentation", property = "openapi.generator.maven.plugin.generateApiDocumentation", required = false)
    protected Boolean generateApiDocumentation = true;

    /**
     * Generate the api documentation.
     */
    @Parameter(name = "withXml", property = "openapi.generator.maven.plugin.withXml", required = false)
    protected Boolean withXml = false;

    /**
     * Skip the execution.
     */
    @Parameter(name = "skip", property = "codegen.skip", required = false, defaultValue = "false")
    protected boolean skip;

    /**
     * Skip the execution if the source file is older than the output folder.
     */
    @Parameter(name = "skipIfSpecIsUnchanged", property = "codegen.skipIfSpecIsUnchanged", required = false, defaultValue = "false")
    protected boolean skipIfSpecIsUnchanged;

    /**
     * Add the output directory to the project as a source root, so that the generated java types
     * are compiled and included in the project artifact.
     */
    @Parameter(defaultValue = "true", property = "openapi.generator.maven.plugin.addCompileSourceRoot")
    protected boolean addCompileSourceRoot = true;

    /**
     * Add the output directory to the project as a test source root, so that the generated java types
     * are compiled only for the test classpath of the project. Setting this parameter will ignore
     * {@link #addCompileSourceRoot}.
     */
    @Parameter(defaultValue = "false", property = "openapi.generator.maven.plugin.addTestCompileSourceRoot")
    protected boolean addTestCompileSourceRoot;

    @Parameter
    protected Map<String, String> environmentVariables = new HashMap<>();

    @Parameter
    protected Map<String, String> originalEnvironmentVariables = new HashMap<>();

    @Parameter(property = "codegen.configHelp")
    protected boolean configHelp = false;

    /**
     * Inline referenced simple type schemas. Simple type extensions are ignored during documentation generation.
     */
    @Parameter(property = "openapi.generator.maven.plugin.unAlias")
    protected boolean unAlias;

    /**
     * Deference components/schemas properties. html2 document generator does not like.
     */
    @Parameter(property = "openapi.generator.maven.plugin.dereferenceComponents")
    protected boolean dereferenceComponents;

    /**
     * Deference components/schemas properties. html2 document generator does not like.
     */
    @Parameter(property = "openapi.generator.maven.plugin.bundlesSpecs")
    protected boolean bundleSpecs;

    @Parameter(name = "writeDebugFiles")
    protected boolean writeDebugFiles = false;

    @Parameter(name = "openapiNormalizer", property = "openapi.generator.maven.plugin.openapiNormalizer")
    private List<String> openapiNormalizer;

    /**
     * A map of scheme and the new one
     */
    @Parameter(name = "schemaMappings", property = "openapi.generator.maven.plugin.schemaMappings")
    private List<String> schemaMappings;

    public void setBuildContext(BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    @SuppressWarnings({"java:S3776", "java:S1874"})
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Code generation is skipped.");
            return;
        }


        super.execute();

        File inputSpecFile = new File(inputSpec);
        File inputParent = inputSpecFile.getParentFile();

        if (inputParent.isDirectory()) {
            try {
                String[] files = Utils.selectInputs(inputParent.toPath(), inputSpecFile.getName());

                switch (files.length) {
                    case 0:
                        throw new MojoExecutionException(
                            format("Input spec %s doesn't match any local file", inputSpec));

                    case 1:
                        inputSpecFile = new File(inputParent, files[0]);
                        inputSpec = inputSpecFile.getAbsolutePath();
                        break;

                    default:
                        String message = format("Input spec %s matches more than one single file", inputSpec);
                        getLog().error(message);
                        Stream.of(files).forEach(f -> getLog().error(format("    %s", f)));
                        throw new MojoExecutionException(
                            format("Input spec %s matches more than one single file", inputSpec));
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot find input " + inputSpec);
            }
        }

        addCompileSourceRootIfConfigured();

        try {
            if (buildContext != null && buildContext.isIncremental() && inputSpec != null && inputSpecFile.exists()
                && !buildContext.hasDelta(inputSpecFile)) {
                getLog().info(
                    "Code generation is skipped in delta-build because source-json was not modified.");
                return;
            }

            if (skipIfSpecIsUnchanged && inputSpecFile.exists()) {
                File storedInputSpecHashFile = getHashFile(inputSpecFile);
                if (storedInputSpecHashFile.exists()) {
                    String inputSpecHash = calculateInputSpecHash(inputSpecFile);
                    String storedInputSpecHash = Files.asCharSource(storedInputSpecHashFile, StandardCharsets.UTF_8)
                        .read();
                    if (inputSpecHash.equals(storedInputSpecHash)) {
                        getLog().info(
                            "Code generation is skipped because input was unchanged");
                        return;
                    }
                }
            }

            // Copy openapi input spec to location.
            if (inputSpecFile.exists() && copyTo != null) {
                getLog().info("Copying input spec to: " + copyTo);
                copyTo.mkdirs();
                java.nio.file.Files.copy(inputSpecFile.toPath(), copyTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            final CodegenConfigurator configurator = loadCodegenConfigurator();

            configurator.setVerbose(verbose);

            if (skipOverwrite != null) {
                configurator.setSkipOverwrite(skipOverwrite);
            }

            if (removeOperationIdPrefix != null) {
                configurator.setRemoveOperationIdPrefix(removeOperationIdPrefix);
            }

            if (isNotEmpty(inputSpec)) {
                if (isValidURI(inputSpec)) {
                    configurator.setInputSpec(inputSpec);
                } else if (inputSpecFile.exists()) {
                    configurator.setInputSpec(inputSpecFile.getAbsoluteFile().toURI().toString());
                } else {
                    throw new MojoExecutionException(inputSpec + " is not a valid URI or file!");
                }
            }

            if (isNotEmpty(gitHost)) {
                configurator.setGitHost(gitHost);
            }

            if (isNotEmpty(gitUserId)) {
                configurator.setGitUserId(gitUserId);
            }

            if (isNotEmpty(gitRepoId)) {
                configurator.setGitRepoId(gitRepoId);
            }

            if (isNotEmpty(ignoreFileOverride)) {
                configurator.setIgnoreFileOverride(ignoreFileOverride);
            }

            if (isNotEmpty(httpUserAgent)) {
                configurator.setHttpUserAgent(httpUserAgent);
            }

            if (skipValidateSpec != null) {
                configurator.setValidateSpec(!skipValidateSpec);
            }

            if (strictSpec != null) {
                configurator.setStrictSpecBehavior(strictSpec);
            }

            if (logToStderr != null) {
                configurator.setLogToStderr(logToStderr);
            }

            if (enablePostProcessFile != null) {
                configurator.setEnablePostProcessFile(enablePostProcessFile);
            }

            if (generateAliasAsModel != null) {
                configurator.setGenerateAliasAsModel(generateAliasAsModel);
            }

            if (isNotEmpty(generatorName)) {
                switch (generatorName) {
                    case "java":
                    case "spring":
                        generatorName = "boat-" + generatorName;
                        break;

                    case "html2":
                        generatorName = "boat-docs";
                        break;
                    case "boat-swift5":
                        generatorName = "boat-swift5";
                        break;
                    default:
                        // use the original generator
                }

                configurator.setGeneratorName(generatorName);


                // check if generatorName & language are set together, inform user this needs to be updated to prevent future issues.
                if (isNotEmpty(language)) {
                    log.warn(
                        "The 'language' option is deprecated and was replaced by 'generatorName'. Both can not be set together");
                    throw new MojoExecutionException(
                        "Illegal configuration: 'language' and  'generatorName' can not be set both, remove 'language' from your configuration");
                }
            } else if (isNotEmpty(language)) {
                log.warn(
                    "The 'language' option is deprecated and may reference language names only in the next major release (4.0). Please use 'generatorName' instead.");
                configurator.setGeneratorName(language);
            } else {
                log.error("A generator name (generatorName) is required.");
                throw new MojoExecutionException(
                    "The generator requires 'generatorName'. Refer to documentation for a list of options.");
            }

            configurator.setOutputDir(output.getAbsolutePath());

            if (isNotEmpty(auth)) {
                configurator.setAuth(auth);
            }

            if (isNotEmpty(apiPackage)) {
                configurator.setApiPackage(apiPackage);
            }

            if (isNotEmpty(modelPackage)) {
                configurator.setModelPackage(modelPackage);
            }

            if (isNotEmpty(invokerPackage)) {
                configurator.setInvokerPackage(invokerPackage);
            }

            if (isNotEmpty(packageName)) {
                configurator.setPackageName(packageName);
            }

            if (isNotEmpty(groupId)) {
                configurator.setGroupId(groupId);
            }

            if (isNotEmpty(artifactId)) {
                configurator.setArtifactId(artifactId);
            }

            if (isNotEmpty(artifactVersion)) {
                configurator.setArtifactVersion(artifactVersion);
            }

            if (isNotEmpty(library)) {
                configurator.setLibrary(library);
            }

            if (isNotEmpty(apiNameSuffix)) {
                configurator.setApiNameSuffix(apiNameSuffix);
            }

            if (isNotEmpty(modelNamePrefix)) {
                configurator.setModelNamePrefix(modelNamePrefix);
            }

            if (isNotEmpty(modelNameSuffix)) {
                configurator.setModelNameSuffix(modelNameSuffix);
            }

            if (null != templateDirectory) {
                configurator.setTemplateDir(templateDirectory.getAbsolutePath());
            }

            if (null != engine) {
                configurator.setTemplatingEngineName(engine);
            }

            // Set generation options
            if (null != generateApis && generateApis) {
                GlobalSettings.setProperty(CodegenConstants.APIS, trimCSV(apisToGenerate));
            } else {
                GlobalSettings.clearProperty(CodegenConstants.APIS);
            }

            if (null != generateModels && generateModels) {
                GlobalSettings.setProperty(CodegenConstants.MODELS, trimCSV(modelsToGenerate));
            } else {
                GlobalSettings.clearProperty(CodegenConstants.MODELS);
            }

            String generatorSupportingFilesToGenerate = Optional.ofNullable(getGeneratorSpecificSupportingFiles())
                .map(GenerateMojo::uniqueJoin)
                .orElse("");

            if (generateSupportingFiles != null && generateSupportingFiles) {
                String allToGenerate = Stream.of(trimCSV(supportingFilesToGenerate), generatorSupportingFilesToGenerate)
                    .filter(StringUtils::isNotBlank)
                    .collect(joining(","));
                GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, allToGenerate);
            } else if (StringUtils.isNotBlank(generatorSupportingFilesToGenerate)) {
                GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, generatorSupportingFilesToGenerate);
            } else {
                GlobalSettings.clearProperty(CodegenConstants.SUPPORTING_FILES);
            }

            GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, generateModelTests.toString());
            GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, generateModelDocumentation.toString());
            GlobalSettings.setProperty(CodegenConstants.API_TESTS, generateApiTests.toString());
            GlobalSettings.setProperty(CodegenConstants.API_DOCS, generateApiDocumentation.toString());
            GlobalSettings.setProperty(CodegenConstants.WITH_XML, withXml.toString());

            if (configOptions != null) {
                // Retained for backwards-compatibility with configOptions -> instantiation-types
                if (instantiationTypes == null && configOptions.containsKey(INSTANTIATION_TYPES)) {
                    applyInstantiationTypesKvp(configOptions.get(INSTANTIATION_TYPES).toString(),
                        configurator);
                }

                // Retained for backwards-compatibility with configOptions -> import-mappings
                if (importMappings == null && configOptions.containsKey(IMPORT_MAPPINGS)) {
                    applyImportMappingsKvp(configOptions.get(IMPORT_MAPPINGS).toString(),
                        configurator);
                }

                // Retained for backwards-compatibility with configOptions -> type-mappings
                if (typeMappings == null && configOptions.containsKey(TYPE_MAPPINGS)) {
                    applyTypeMappingsKvp(configOptions.get(TYPE_MAPPINGS).toString(), configurator);
                }

                // Retained for backwards-compatibility with configOptions -> language-specific-primitives
                if (languageSpecificPrimitives == null && configOptions.containsKey(LANGUAGE_SPECIFIC_PRIMITIVES)) {
                    applyLanguageSpecificPrimitivesCsv(configOptions
                        .get(LANGUAGE_SPECIFIC_PRIMITIVES).toString(), configurator);
                }

                // Retained for backwards-compatibility with configOptions -> additional-properties
                if (additionalProperties == null && configOptions.containsKey(ADDITIONAL_PROPERTIES)) {
                    applyAdditionalPropertiesKvp(configOptions.get(ADDITIONAL_PROPERTIES).toString(),
                        configurator);
                }

                if (serverVariableOverrides == null && configOptions.containsKey(SERVER_VARIABLES)) {
                    applyServerVariablesKvp(configOptions.get(SERVER_VARIABLES).toString(), configurator);
                }

                // Retained for backwards-compatibility with configOptions -> reserved-words-mappings
                if (reservedWordsMappings == null && configOptions.containsKey(RESERVED_WORDS_MAPPINGS)) {
                    applyReservedWordsMappingsKvp(configOptions.get(RESERVED_WORDS_MAPPINGS)
                        .toString(), configurator);
                }

                // Retained for backwards-compatibility with configOptions -> schema-mappings
                if (schemaMappings == null && configOptions.containsKey(SCHEMA_MAPPING)) {
                    applySchemaMappingsKvp(configOptions.get(SCHEMA_MAPPING).toString(),
                        configurator);
                }
            }

            // Apply Instantiation Types
            if (instantiationTypes != null && (configOptions == null || !configOptions.containsKey(
                INSTANTIATION_TYPES))) {
                applyInstantiationTypesKvpList(instantiationTypes, configurator);
            }

            // Apply Import Mappings
            if (importMappings != null && (configOptions == null || !configOptions.containsKey(IMPORT_MAPPINGS))) {
                applyImportMappingsKvpList(importMappings, configurator);
            }

            // Apply Type Mappings
            if (typeMappings != null && (configOptions == null || !configOptions.containsKey(TYPE_MAPPINGS))) {
                applyTypeMappingsKvpList(typeMappings, configurator);
            }

            // Apply Language Specific Primitives
            if (languageSpecificPrimitives != null
                && (configOptions == null || !configOptions.containsKey(LANGUAGE_SPECIFIC_PRIMITIVES))) {
                applyLanguageSpecificPrimitivesCsvList(languageSpecificPrimitives, configurator);
            }

            // Apply Additional Properties
            if (additionalProperties != null && (configOptions == null || !configOptions.containsKey(
                ADDITIONAL_PROPERTIES))) {
                applyAdditionalPropertiesKvpList(additionalProperties, configurator);
            }

            if (serverVariableOverrides != null && (configOptions == null || !configOptions.containsKey(
                SERVER_VARIABLES))) {
                applyServerVariablesKvpList(serverVariableOverrides, configurator);
            }

            // Apply Reserved Words Mappings
            if (reservedWordsMappings != null && (configOptions == null || !configOptions.containsKey(
                RESERVED_WORDS_MAPPINGS))) {
                applyReservedWordsMappingsKvpList(reservedWordsMappings, configurator);
            }

            if (openapiNormalizer != null && (configOptions == null || !configOptions.containsKey("openapi-normalizer"))) {
                for (String propString: openapiNormalizer) {
                    OptionUtils.parseCommaSeparatedTuples(propString)
                            .forEach(p -> {configurator.addOpenapiNormalizer(p.getLeft(), p.getRight());});
                }
            }

            // Apply Schema Mappings
            if (schemaMappings != null && (configOptions == null || !configOptions.containsKey(SCHEMA_MAPPING))) {
                applySchemaMappingsKvpList(schemaMappings, configurator);
            }

            if (environmentVariables != null) {

                for (Entry<String, String> entry : environmentVariables.entrySet()) {
                    String key = entry.getKey();
                    originalEnvironmentVariables.put(key, GlobalSettings.getProperty(key));
                    String value = environmentVariables.get(key);
                    if (value == null) {
                        // don't put null values
                        value = "";
                    }
                    GlobalSettings.setProperty(key, value);
                    configurator.addGlobalProperty(key, value);
                }
            }

            final ClientOptInput input = configurator.toClientOptInput();
            final CodegenConfig config = input.getConfig();

            if (configOptions != null) {
                for (CliOption langCliOption : config.cliOptions()) {
                    if (configOptions.containsKey(langCliOption.getOpt())) {
                        input.getConfig().additionalProperties()
                            .put(langCliOption.getOpt(), configOptions.get(langCliOption.getOpt()));
                    }
                }
            }

            if (configHelp) {
                for (CliOption langCliOption : config.cliOptions()) {
                    getLog().info("\t" + langCliOption.getOpt());
                    getLog().info("\t    "
                        + langCliOption.getOptionHelp().replace("\n", "\n\t    "));
                }
                return;
            }
            adjustAdditionalProperties(config);

            if (unAlias) {
                new UnAliasTransformer().transform(input.getOpenAPI(), emptyMap());
                if(writeDebugFiles) {
                    java.nio.file.Files.write(new File(output, "openapi-unaliased.yaml").toPath(), Yaml.pretty(input.getOpenAPI()).getBytes(StandardCharsets.UTF_8));
                }
            }
            if (dereferenceComponents) {
                new DereferenceComponentsPropertiesTransformer().transform(input.getOpenAPI(), emptyMap());
                if(writeDebugFiles) {
                    java.nio.file.Files.write(new File(output, "openapi-dereferenced.yaml").toPath(), Yaml.pretty(input.getOpenAPI()).getBytes(StandardCharsets.UTF_8));
                }
            }

            if(bundleSpecs) {
                new Bundler(inputSpecFile).transform(input.getOpenAPI(), Collections.emptyMap());

                if(writeDebugFiles) {
                    java.nio.file.Files.write(new File(output, "openapi-bundled.yaml").toPath(), Yaml.pretty(input.getOpenAPI()).getBytes(StandardCharsets.UTF_8));
                }
            }


            new DefaultGenerator().opts(input).generate();

            if (buildContext != null) {
                buildContext.refresh(new File(getCompileSourceRoot()));
            }

            // Store a checksum of the input spec
            File storedInputSpecHashFile = getHashFile(inputSpecFile);
            String inputSpecHash = calculateInputSpecHash(inputSpecFile);

            if (storedInputSpecHashFile.getParent() != null && !new File(storedInputSpecHashFile.getParent()).exists()) {
                File parent = new File(storedInputSpecHashFile.getParent());
                parent.mkdirs();
            }
            Files.asCharSink(storedInputSpecHashFile, StandardCharsets.UTF_8).write(inputSpecHash);

        } catch (Exception e) {
            // Maven logs exceptions thrown by plugins only if invoked with -e
            // I find it annoying to jump through hoops to get basic diagnostic information,
            // so let's log it in any case:
            if (buildContext != null) {
                buildContext.addError(inputSpecFile, 0, 0, "unexpected error in Open-API generation", e);
            }
            getLog().error(e);
            throw new MojoExecutionException(
                "Code generation failed. See above for the full exception.");
        }
    }

    /**
     * Attempt to read from config file, return default otherwise.
     *
     * @return The CodegenConfigurator loaded from file or a default.
     */
    private CodegenConfigurator loadCodegenConfigurator() {
        CodegenConfigurator configurator = CodegenConfigurator.fromFile(configurationFile);
        return configurator != null ? configurator : new CodegenConfigurator();
    }

    protected Collection<String> getGeneratorSpecificSupportingFiles() {
        return Collections.emptySet();
    }

    /**
     * Calculate openapi specification file hash. If specification is hosted on remote resource it is downloaded first
     *
     * @param inputSpecFile - Openapi specification input file to calculate its hash.
     *                      Does not take into account if input spec is hosted on remote resource
     * @return openapi specification file hash
     * @throws IOException When cannot read the file
     */
    @SuppressWarnings({"java:S2095", "java:S4790", "java:S5361"})
    private String calculateInputSpecHash(File inputSpecFile) throws IOException {

        URL inputSpecRemoteUrl = inputSpecRemoteUrl();

        File inputSpecTempFile = inputSpecFile;

        if (inputSpecRemoteUrl != null) {
            inputSpecTempFile = File.createTempFile("openapi-spec", ".tmp");

            URLConnection conn = inputSpecRemoteUrl.openConnection();
            if (isNotEmpty(auth)) {
                List<AuthorizationValue> authList = AuthParser.parse(auth);
                for (AuthorizationValue authorizationValue : authList) {
                    conn.setRequestProperty(authorizationValue.getKeyName(), authorizationValue.getValue());
                }
            }
            ReadableByteChannel readableByteChannel = Channels.newChannel(conn.getInputStream());

            try (FileOutputStream fileOutputStream = new FileOutputStream(inputSpecTempFile)) {
                FileChannel fileChannel = fileOutputStream.getChannel();
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            } catch (FileNotFoundException e) {
                throw new IOException(e);
            }

        }

        ByteSource inputSpecByteSource =
            inputSpecTempFile.exists()
                ? Files.asByteSource(inputSpecTempFile)
                : CharSource
                .wrap(ClasspathHelper.loadFileFromClasspath(inputSpecTempFile.toString().replaceAll("\\{2}", "/")))
                .asByteSource(StandardCharsets.UTF_8);

        return inputSpecByteSource.hash(Hashing.sha256()).toString();
    }

    /**
     * Try to parse inputSpec setting string into URL.
     *
     * @return A valid URL or null if inputSpec is not a valid URL
     */
    private URL inputSpecRemoteUrl() {
        try {
            return new URI(inputSpec).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get specification hash file.
     *
     * @param inputSpecFile - Openapi specification input file  to calculate its hash.
     *                      Does not take into account if input spec is hosted on remote resource
     * @return a file with previously calculated hash
     */
    private File getHashFile(File inputSpecFile) {
        String name = inputSpecFile.getName();

        URL url = inputSpecRemoteUrl();
        if (url != null) {
            String[] segments = url.getPath().split("/");
            name = Files.getNameWithoutExtension(segments[segments.length - 1]);
        }

        return new File(output.getPath() + File.separator + ".openapi-generator" + File.separator + name + ".sha256");
    }

    private String getCompileSourceRoot() {
        final Object sourceFolderObject =
            configOptions == null ? null : configOptions
                .get(CodegenConstants.SOURCE_FOLDER);
        final String sourceFolder;
        if (sourceFolderObject != null) {
            sourceFolder = sourceFolderObject.toString();
        } else {
            sourceFolder = addTestCompileSourceRoot ? "src/test/java" : "src/main/java";
        }

        return output.toString() + "/" + sourceFolder;
    }

    private void addCompileSourceRootIfConfigured() {
        if (addTestCompileSourceRoot) {
            project.addTestCompileSourceRoot(getCompileSourceRoot());
        } else if (addCompileSourceRoot) {
            project.addCompileSourceRoot(getCompileSourceRoot());
        }

        // Reset all environment variables to their original value. This prevents unexpected
        // behaviour
        // when running the plugin multiple consecutive times with different configurations.
        for (Map.Entry<String, String> entry : originalEnvironmentVariables.entrySet()) {
            if (entry.getValue() == null) {
                GlobalSettings.clearProperty(entry.getKey());
            } else {
                GlobalSettings.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * This method enables conversion of true/false strings in
     * config.additionalProperties (configuration/configOptions) to proper booleans.
     * This enables mustache files to handle the properties better.
     *
     * @param config CodeGen config
     */
    private void adjustAdditionalProperties(final CodegenConfig config) {
        Map<String, Object> configAdditionalProperties = config.additionalProperties();
        Set<String> keySet = configAdditionalProperties.keySet();
        for (String key : keySet) {
            Object value = configAdditionalProperties.get(key);
            if (value != null) {
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (stringValue.equalsIgnoreCase("true")) {
                        configAdditionalProperties.put(key, Boolean.TRUE);
                    } else if (stringValue.equalsIgnoreCase("false")) {
                        configAdditionalProperties.put(key, Boolean.FALSE);
                    }
                }
            } else {
                configAdditionalProperties.put(key, Boolean.FALSE);
            }
        }
    }

    private static boolean isValidURI(String urlString) {
        try {
            new URI(urlString);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

}
