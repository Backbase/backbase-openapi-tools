package com.backbase.oss.boat.quay;

import com.backbase.oss.boat.quay.configuration.RulesValidatorConfiguration;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.zalando.zally.core.ApiValidator;

public class BoatLinterTests {

    @Test
    public void lint() throws IOException {
        ApiValidator apiValidator = RulesValidatorConfiguration.defaultApiValidator();
        BoatLinter boatLinter = new BoatLinter(apiValidator);

        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());
        boatLinter.lint(openApiContents);
    }

}
