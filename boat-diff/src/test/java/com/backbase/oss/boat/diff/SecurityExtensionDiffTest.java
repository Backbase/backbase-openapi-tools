package com.backbase.oss.boat.diff;

import static com.backbase.oss.boat.diff.TestUtils.assertOpenApiChangedEndpoints;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.backbase.oss.boat.diff.model.ChangedOAuthFlow;
import com.backbase.oss.boat.diff.model.ChangedOpenApi;
import com.backbase.oss.boat.diff.model.ChangedOperation;
import com.backbase.oss.boat.diff.model.ChangedSecurityRequirement;
import com.backbase.oss.boat.diff.model.ChangedSecurityRequirements;
import com.backbase.oss.boat.diff.model.ChangedSecuritySchemeScopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Test;

/**
 * Created by adarsh.sharma on 06/01/18.
 */
public class SecurityExtensionDiffTest {
    private final String OPENAPI_DOC1 = "security_diff_1.yaml";
    private final String OPENAPI_DOC2 = "security_diff_2.yaml";

    @Test
    public void testDiffDifferent() {
        assertOpenApiChangedEndpoints(OPENAPI_DOC1, OPENAPI_DOC2);
    }
}
