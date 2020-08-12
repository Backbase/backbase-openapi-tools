package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate-webclient-embedded", threadSafe = true)
@Slf4j
public class GenerateWebClientEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    protected boolean isGenerateSupportingFiles() {
        return true;
    }

    @Override
    protected String getLibrary() {
        return "webclient";
    }

    @Override
    protected boolean isEmbedded() {
        return true;
    }

    @Override
    protected Boolean isReactive() {
        return false;
    }


}
