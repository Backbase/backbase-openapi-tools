package com.backbase.oss.codegen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.api.TemplatePathLocator;
import org.openapitools.codegen.api.TemplatingEngineAdapter;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.templating.CommonTemplateContentLocator;
import org.openapitools.codegen.templating.GeneratorTemplateContentLocator;
import org.openapitools.codegen.templating.MustacheEngineAdapter;
import org.openapitools.codegen.templating.TemplateManagerOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class NonOpenApiGenerator implements Generator {


    protected CodegenConfig config;
    protected String input;
    protected ClientOptInput opts;
    protected TemplateManager templateProcessor;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Generator opts(ClientOptInput opts) {
        this.opts = opts;
        this.input = opts.getConfig().getInputSpec();
        this.config = opts.getConfig();
        TemplateManagerOptions templateManagerOptions = new TemplateManagerOptions(
                this.config.isEnableMinimalUpdate(),
                this.config.isSkipOverwrite());

        TemplatingEngineAdapter templatingEngine = this.config.getTemplatingEngine();

        if (templatingEngine instanceof MustacheEngineAdapter) {
            MustacheEngineAdapter mustacheEngineAdapter = (MustacheEngineAdapter) templatingEngine;
            mustacheEngineAdapter.setCompiler(this.config.processCompiler(mustacheEngineAdapter.getCompiler()));
        }


        TemplatePathLocator commonTemplateLocator = new CommonTemplateContentLocator();
        TemplatePathLocator generatorTemplateLocator = new GeneratorTemplateContentLocator(this.config);
        this.templateProcessor = new TemplateManager(
                templateManagerOptions,
                templatingEngine,
                new TemplatePathLocator[]{generatorTemplateLocator, commonTemplateLocator}
        );
        return this;
    }

    @Override
    public List<File> generate() {


        return null;
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
                processSupport(support, supportingFilesToGenerate, bundle, files);
            } catch (Exception e) {
                throw new CodegenException("Could not generate supporting file '" + support + "'", e);
            }
        }
        return files;
    }

    private void processSupport(SupportingFile support, Set<String> supportingFilesToGenerate, Map<String, Object> bundle, List<File> files) throws IOException {
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
        File generated = processTemplateToFile(bundle, support.getTemplateFile(), outputFilename, true, "");
        files.add(generated);

//        String templateFile  =  templateProcessor.getFullTemplateFile(config, support.templateFile);
//
//        if (!shouldGenerate(supportingFilesToGenerate, support)) {
//            return;
//        }
//
//        if (ignoreProcessor.allowsFile(new File(outputFilename))) {
//            ignoreProcessorAllowsFile(support, outputFilename, bundle, files, templateFile);
//
//        } else {
//            log.info("Skipped generation of {} due to rule in .openapi-generator-ignore", outputFilename);
//        }
    }

    protected File processTemplateToFile(Map<String, Object> templateData, String templateName, String outputFilename, boolean shouldGenerate, String skippedByOption) throws IOException {
        String adjustedOutputFilename = outputFilename.replaceAll("//", "/").replace('/', File.separatorChar);
        File target = new File(adjustedOutputFilename);
        if (shouldGenerate) {
            Path outDir = java.nio.file.Paths.get(this.config.getOutputDir()).toAbsolutePath();
            Path absoluteTarget = target.toPath().toAbsolutePath();
            if (!absoluteTarget.startsWith(outDir)) {
                throw new RuntimeException(String.format(Locale.ROOT, "Target files must be generated within the output directory; absoluteTarget=%s outDir=%s", absoluteTarget, outDir));
            }
            return this.templateProcessor.write(templateData, templateName, target);
        } else {
            this.templateProcessor.skip(target.toPath(), String.format(Locale.ROOT, "Skipped by %s options supplied by user.", skippedByOption));
            return null;
        }
    }


    @SuppressWarnings("unchecked")
    protected Map<String, Object> convertToBundle(Object object) throws JsonProcessingException {
        String model = objectMapper.writeValueAsString(object);
        return objectMapper.readValue(model, Map.class);
    }
}
