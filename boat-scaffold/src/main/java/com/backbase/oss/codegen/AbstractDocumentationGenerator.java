package com.backbase.oss.codegen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.api.TemplatingEngineAdapter;
import org.openapitools.codegen.ignore.CodegenIgnoreProcessor;
import org.openapitools.codegen.templating.HandlebarsEngineAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public abstract class AbstractDocumentationGenerator implements Generator {

    protected final CodegenConfig config;
    protected final String input;
    protected final String output;

    protected TemplateManager templateProcessor;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected CodegenIgnoreProcessor ignoreProcessor;
    private final TemplatingEngineAdapter templatingEngine = new HandlebarsEngineAdapter();

    protected AbstractDocumentationGenerator(CodegenConfig config) {
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
        for (SupportingFile support : config.supportingFiles()) {
            try {
                processSupport(support, bundle, files);
            } catch (Exception e) {
                throw new CodegenException("Could not generate supporting file '" + support + "'", e);
            }
        }
        return files;
    }

    private void processSupport(SupportingFile support, Map<String, Object> bundle, List<File> files) throws IOException {
        String outputFolder = config.outputFolder();
        if (StringUtils.isNotEmpty(support.getFolder())) {
            outputFolder += File.separator + support.getFolder();
        }
        File of = new File(outputFolder);
        if (!of.isDirectory() && !of.mkdirs()) {
            log.debug("Output directory {} not created. It {}.", outputFolder, of.exists() ? "already exists." : "may not have appropriate permissions.");

        }
        String outputFilename = new File(support.getDestinationFilename()).isAbsolute() // split
                ? support.getDestinationFilename()
                : outputFolder + File.separator + support.getDestinationFilename().replace('/', File.separatorChar);
        if (!config.shouldOverwrite(outputFilename)) {
            log.info("Skipped overwriting {}", outputFilename);
            return;
        }
        File generated = processTemplateToFile(bundle, support.getTemplateFile(), outputFilename);
        files.add(generated);

    }



    protected File processTemplateToFile(Map<String, Object> templateData, String templateName, String outputFilename) throws IOException {
        String adjustedOutputFilename = outputFilename.replace("//", "/").replace('/', File.separatorChar);
        File target = new File(adjustedOutputFilename);
        Path outDir = java.nio.file.Paths.get(this.config.getOutputDir()).toAbsolutePath();
        Path absoluteTarget = target.toPath().toAbsolutePath();
        if (!absoluteTarget.startsWith(outDir)) {
            throw new CodegenException(String.format(Locale.ROOT, "Target files must be generated within the output directory; absoluteTarget=%s outDir=%s", absoluteTarget, outDir));
        }
        return this.templateProcessor.write(templateData, templateName, target);
    }


}
