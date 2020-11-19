package com.backbase.oss.codegen.lint;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.configuration.RulesValidatorConfiguration;
import com.backbase.oss.codegen.yard.model.LintRuleReport;
import com.backbase.oss.codegen.yard.model.Portal;
import com.backbase.oss.codegen.yard.model.YardModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.AbstractGenerator;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.Generator;
import org.openapitools.codegen.GlobalSupportingFile;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.api.TemplatingEngineAdapter;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.ignore.CodegenIgnoreProcessor;
import org.openapitools.codegen.templating.HandlebarsEngineAdapter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.DefaultContextFactory;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RulesManager;


@Slf4j
public class BoatLintGenerator extends AbstractGenerator implements Generator {

    private final BoatLintConfig config;
    private final String inputSpec;
    private final String output;

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected CodegenIgnoreProcessor ignoreProcessor;
    private final TemplatingEngineAdapter templatingEngine = new HandlebarsEngineAdapter();
    private YardModel yardModel;

    public BoatLintGenerator(BoatLintConfig config) {
        this.config = config;
        this.inputSpec = config.getInputSpec();
        this.output = config.getOutputDir();
        if (this.ignoreProcessor == null) {
            this.ignoreProcessor = new CodegenIgnoreProcessor(this.config.getOutputDir());
        }
    }


    @Override
    public Generator opts(ClientOptInput opts) {
        return this;
    }

    public List<File> generate() {


        RulesValidatorConfiguration rulesValidatorConfiguration = new RulesValidatorConfiguration();
        Config config = rulesValidatorConfiguration.config("boat.conf");
        RulesManager rulesManager = rulesValidatorConfiguration.rulesManager(config);
        ApiValidator apiValidator = rulesValidatorConfiguration.apiValidator(rulesManager, new DefaultContextFactory());
        BoatLinter boatLinter = new BoatLinter((apiValidator));

        File input = new File(inputSpec);
        if(!input.exists()) {
            throw new IllegalArgumentException("Input " + input + " does not exist");
        }

        try {
            String openApiAsString = new String(Files.readAllBytes(input.toPath()));
            OpenAPI openAPI = OpenAPILoader.load(input);

            List<Result> results = boatLinter.lint(openApiAsString);

            LintRuleReport lintRuleReport = new LintRuleReport();
            lintRuleReport.setOpenApi(openApiAsString);
            lintRuleReport.setResults(results);
            Info info = openAPI.getInfo();
            if(info != null) {
                lintRuleReport.setTitle(info.getTitle());
                lintRuleReport.setVersion(info.getVersion());
            }

            return generate(lintRuleReport);
        } catch (OpenAPILoaderException | IOException e) {
            throw new IllegalArgumentException("Cannot read file: " + inputSpec, e);
        }
    }

    @SneakyThrows
    private List<File> generate(LintRuleReport lintRuleReport) {
        log.info("Generating BOAT Lint Report for: {}", lintRuleReport.getTitle());

        // After processing our model, convert it into a map;
        Map<String, Object> bundle = covertPortalToBundle(lintRuleReport);
        List<File> files = processTemplates(bundle);
        log.info("Finished creating BOAT Yard for portal: {}", lintRuleReport.getTitle());

        return files;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> covertPortalToBundle(LintRuleReport lintRuleReport) throws JsonProcessingException {
        String model = objectMapper.writeValueAsString(lintRuleReport);
        return objectMapper.readValue(model, Map.class);
    }

    private List<File> processTemplates(Map<String, Object> bundle) {
        List<File> files = new ArrayList<>();
        Set<String> supportingFilesToGenerate = null;
        String supportingFiles = GlobalSettings.getProperty(CodegenConstants.SUPPORTING_FILES);
        if (supportingFiles != null && !supportingFiles.isEmpty()) {
            supportingFilesToGenerate = new HashSet<>(Arrays.asList(supportingFiles.split(",")));
        }


        for (SupportingFile support : config.supportingFiles()) {
            try {
                String outputFolder = config.outputFolder();
                if (StringUtils.isNotEmpty(support.folder)) {
                    outputFolder += File.separator + support.folder;
                }
                File of = new File(outputFolder);
                if (!of.isDirectory()) {
                    if (!dryRun && !of.mkdirs()) {
                        log.debug("Output directory {} not created. It {}.", outputFolder, of.exists() ? "already exists." : "may not have appropriate permissions.");
                    }
                }
                String outputFilename = new File(support.destinationFilename).isAbsolute() // split
                    ? support.destinationFilename
                    : outputFolder + File.separator + support.destinationFilename.replace('/', File.separatorChar);
                if (!config.shouldOverwrite(outputFilename)) {
                    log.info("Skipped overwriting {}", outputFilename);
                    continue;
                }
                String templateFile;
                if (support instanceof GlobalSupportingFile) {
                    templateFile = config.getCommonTemplateDir() + File.separator + support.templateFile;
                } else {
                    templateFile = getFullTemplateFile(config, support.templateFile);
                }
                boolean shouldGenerate = true;
                if (supportingFilesToGenerate != null && !supportingFilesToGenerate.isEmpty()) {
                    shouldGenerate = supportingFilesToGenerate.contains(support.destinationFilename);
                }
                if (!shouldGenerate) {
                    continue;
                }

                if (ignoreProcessor.allowsFile(new File(outputFilename))) {
                    // support.templateFile is the unmodified/original supporting file name (e.g. build.sh.mustache)
                    // templatingEngine.templateExists dispatches resolution to this, performing template-engine specific inspect of support file extensions.
                    if (templatingEngine.templateExists(this, support.templateFile)) {
                        String templateContent = templatingEngine.compileTemplate(this, bundle, support.templateFile);
                        writeToFile(outputFilename, templateContent);
                        File written = new File(outputFilename);
                        files.add(written);
                        if (config.isEnablePostProcessFile()) {
                            config.postProcessFile(written, "supporting-mustache");
                        }
                    } else {
                        InputStream in = null;

                        try {
                            in = new FileInputStream(templateFile);
                        } catch (Exception e) {
                            // continue
                        }
                        if (in == null) {
                            in = this.getClass().getClassLoader().getResourceAsStream(getCPResourcePath(templateFile));
                        }
                        File outputFile = writeInputStreamToFile(outputFilename, in, templateFile);
                        files.add(outputFile);
                        if (config.isEnablePostProcessFile() && !dryRun) {
                            config.postProcessFile(outputFile, "supporting-common");
                        }
                    }

                } else {
                    log.info("Skipped generation of {} due to rule in .openapi-generator-ignore", outputFilename);
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not generate supporting file '" + support + "'", e);
            }
        }
        return files;
    }

    protected File writeInputStreamToFile(String filename, InputStream in, String templateFile) throws IOException {
        if (in != null) {
            byte[] bytes = IOUtils.toByteArray(in);
            return writeToFile(filename, bytes);
        } else {
            log.error("can't open '{}' for input; cannot write '{}'", templateFile, filename);
            return null;
        }
    }

    private File processTemplateToFile(Map<String, Object> bundle, Path template) throws IOException {
        String templateContent = templatingEngine.compileTemplate(this, bundle, template.toString());
        String outputFilename = StringUtils.substringBeforeLast(template.getFileName().toString(), ".") + ".html";
        File templateOutputFile = new File(this.output, outputFilename);
        writeToFile(templateOutputFile.toString(), templateContent);
        return templateOutputFile;
    }


    private YardModel getMarinaModel() {
        YardModel yardModel;

        Constructor constructor = new Constructor(YardModel.class);

        Yaml yaml = new Yaml(constructor);
        try {
            yardModel = yaml.loadAs(new FileInputStream(inputSpec), YardModel.class);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create BOAT Yard from input: " + inputSpec, e);
        }
        this.yardModel = yardModel;
        return yardModel;
    }

    @Override
    public boolean getEnableMinimalUpdate() {
        return false;
    }

    @Override
    public String getFullTemplateContents(String templateName) {
        return readTemplate(getFullTemplateFile(config, templateName));
    }

    @Override
    public Path getFullTemplatePath(String name) {
        String fullPath = getFullTemplateFile(config, name);
        return java.nio.file.Paths.get(fullPath);
    }
}
