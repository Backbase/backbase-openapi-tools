package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate-spring-boot-embedded", threadSafe = true)
@Slf4j
public class GenerateSpringBootEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    protected boolean isGenerateSupportingFiles() {
        return false;
    }

    @Override
    protected String getLibrary() {
        return "spring-boot";
    }

    @Override
    protected boolean isEmbedded() {
        return true;
    }

    @Override
    protected Boolean isReactive() {
        return true;
    }


}
