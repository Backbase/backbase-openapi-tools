package com.backbase.oss.boat.diff.compare;

import static java.util.Optional.ofNullable;

import com.backbase.oss.boat.diff.model.ChangedOAuthFlow;
import com.backbase.oss.boat.diff.utils.ChangedUtils;
import io.swagger.v3.oas.models.security.OAuthFlow;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by adarsh.sharma on 12/01/18.
 */
public class OAuthFlowDiff {
    private OpenApiDiff openApiDiff;

    public OAuthFlowDiff(OpenApiDiff openApiDiff) {
        this.openApiDiff = openApiDiff;
    }

    private static Map<String, Object> getExtensions(OAuthFlow oAuthFlow) {
        return ofNullable(oAuthFlow).map(OAuthFlow::getExtensions).orElse(null);
    }

    public Optional<ChangedOAuthFlow> diff(OAuthFlow left, OAuthFlow right) {
        ChangedOAuthFlow changedOAuthFlow = new ChangedOAuthFlow(left, right);
        if (left != null && right != null) {
            changedOAuthFlow
                    .setAuthorizationUrl(
                            !Objects.equals(left.getAuthorizationUrl(), right.getAuthorizationUrl()))
                    .setTokenUrl(!Objects.equals(left.getTokenUrl(), right.getTokenUrl()))
                    .setRefreshUrl(!Objects.equals(left.getRefreshUrl(), right.getRefreshUrl()));
        }
        openApiDiff
                .getExtensionsDiff()
                .diff(getExtensions(left), getExtensions(right))
                .ifPresent(changedOAuthFlow::setExtensions);
        return ChangedUtils.isChanged(changedOAuthFlow);
    }
}
