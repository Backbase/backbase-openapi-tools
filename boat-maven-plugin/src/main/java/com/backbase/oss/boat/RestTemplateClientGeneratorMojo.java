package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-rest-template-embedded", threadSafe = true)
@Slf4j
public class RestTemplateClientGeneratorMojo extends AbstractCodeGenMojo {

    @Override
    protected boolean isGenerateSupportingFiles() {
        return true;
    }

    @Override
    protected String getLibrary() {
        return "resttemplate";
    }

    @Override
    protected boolean isEmbedded() {
        return true;
    }


}
