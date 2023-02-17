package com.backbase.oss.codegen;

import com.backbase.oss.codegen.marina.BoatHandlebarsEngineAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.Generator;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.TemplateManager;
import org.openapitools.codegen.api.TemplatePathLocator;
import org.openapitools.codegen.api.TemplatingEngineAdapter;
import org.openapitools.codegen.templating.CommonTemplateContentLocator;
import org.openapitools.codegen.templating.GeneratorTemplateContentLocator;
import org.openapitools.codegen.templating.TemplateManagerOptions;

@Slf4j
public abstract class AbstractDocumentationGenerator implements Generator {

    protected CodegenConfig config;
    protected String input;
    protected ClientOptInput opts;
    protected TemplateManager templateProcessor;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("deprecation")
    @Override
    public Generator opts(ClientOptInput opts) {
        this.opts = opts;
        this.input = opts.getConfig().getInputSpec();
        this.config = opts.getConfig();
        TemplateManagerOptions templateManagerOptions = new TemplateManagerOptions(
            this.config.isEnableMinimalUpdate(),
            this.config.isSkipOverwrite());

        TemplatingEngineAdapter templatingEngine = new BoatHandlebarsEngineAdapter();

        TemplatePathLocator commonTemplateLocator = new CommonTemplateContentLocator();
        TemplatePathLocator generatorTemplateLocator = new GeneratorTemplateContentLocator(this.config);
        this.templateProcessor = new TemplateManager(
            templateManagerOptions,
            templatingEngine,
            new TemplatePathLocator[]{generatorTemplateLocator, commonTemplateLocator}
        );
        return this;
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

    @SuppressWarnings("unchecked")
    protected Map<String, Object> convertToBundle(Object object) throws JsonProcessingException {
        String model = objectMapper.writeValueAsString(object);
        return objectMapper.readValue(model, Map.class);
    }
}
