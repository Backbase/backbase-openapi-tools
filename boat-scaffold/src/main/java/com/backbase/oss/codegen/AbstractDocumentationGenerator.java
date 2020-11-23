package com.backbase.oss.codegen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.AbstractGenerator;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.Generator;
import org.openapitools.codegen.GlobalSupportingFile;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.api.TemplatingEngineAdapter;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.ignore.CodegenIgnoreProcessor;
import org.openapitools.codegen.templating.HandlebarsEngineAdapter;

@Slf4j
public abstract class AbstractDocumentationGenerator  extends AbstractGenerator implements Generator {

    protected final CodegenConfig config;
    protected final String input;
    protected final String output;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected CodegenIgnoreProcessor ignoreProcessor;
    private final TemplatingEngineAdapter templatingEngine = new HandlebarsEngineAdapter();

    public AbstractDocumentationGenerator(CodegenConfig config) {
        this.config = config;
        this.input = config.getInputSpec();
        this.output = config.getOutputDir();
        if (this.ignoreProcessor == null) {
            this.ignoreProcessor = new CodegenIgnoreProcessor(this.config.getOutputDir());
        }
    }


    @Override
    public Generator opts(ClientOptInput opts) {
        return this;
    }


    @SuppressWarnings("unchecked")
    protected Map<String, Object> convertToBundle(Object object) throws JsonProcessingException {
        String model = objectMapper.writeValueAsString(object);
        return objectMapper.readValue(model, Map.class);
    }

    protected List<File> processTemplates(Map<String, Object> bundle) {
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
                    if (!of.mkdirs()) {
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

    protected File processTemplateToFile(Map<String, Object> bundle, Path template) throws IOException {
        String templateContent = templatingEngine.compileTemplate(this, bundle, template.toString());
        String outputFilename = StringUtils.substringBeforeLast(template.getFileName().toString(), ".") + ".html";
        File templateOutputFile = new File(this.output, outputFilename);
        writeToFile(templateOutputFile.toString(), templateContent);
        return templateOutputFile;
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
