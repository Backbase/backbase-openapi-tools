package com.backbase.oss.boat.diff.compare;

import com.backbase.oss.boat.diff.model.ChangedSecurityRequirement;
import com.backbase.oss.boat.diff.model.ChangedSecurityRequirements;
import com.backbase.oss.boat.diff.model.DiffContext;
import com.backbase.oss.boat.diff.utils.ChangedUtils;
import com.backbase.oss.boat.diff.utils.RefPointer;
import com.backbase.oss.boat.diff.utils.RefType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by adarsh.sharma on 07/01/18.
 */
public class SecurityRequirementsDiff {
    private OpenApiDiff openApiDiff;
    private Components leftComponents;
    private Components rightComponents;
    private static RefPointer<SecurityScheme> refPointer = new RefPointer<>(RefType.SECURITY_SCHEMES);

    public SecurityRequirementsDiff(OpenApiDiff openApiDiff) {
        this.openApiDiff = openApiDiff;
        this.leftComponents =
                openApiDiff.getOldSpecOpenApi() != null
                        ? openApiDiff.getOldSpecOpenApi().getComponents()
                        : null;
        this.rightComponents =
                openApiDiff.getNewSpecOpenApi() != null
                        ? openApiDiff.getNewSpecOpenApi().getComponents()
                        : null;
    }

    public Optional<SecurityRequirement> contains(
            List<SecurityRequirement> securityRequirements, SecurityRequirement left) {
        return securityRequirements.stream()
                .filter(rightSecurities -> same(left, rightSecurities))
                .findFirst();
    }

    public boolean same(SecurityRequirement left, SecurityRequirement right) {
        List<Pair<SecurityScheme.Type, SecurityScheme.In>> leftTypes =
                getListOfSecuritySchemes(leftComponents, left);
        List<Pair<SecurityScheme.Type, SecurityScheme.In>> rightTypes =
                getListOfSecuritySchemes(rightComponents, right);
        return CollectionUtils.isEqualCollection(leftTypes, rightTypes);
    }

    private List<Pair<SecurityScheme.Type, SecurityScheme.In>> getListOfSecuritySchemes(
            Components components, SecurityRequirement securityRequirement) {
        return securityRequirement.keySet().stream()
                .map(
                        x -> {
                            Supplier<IllegalArgumentException> s = () -> new IllegalArgumentException("Impossible to find security scheme: " + x);
                            //SecurityScheme result = components.getSecuritySchemes().get(x);
                            Map<String, SecurityScheme> map =
                                    Optional.ofNullable(components.getSecuritySchemes())
                                    .orElseThrow(s);

                            return Optional.ofNullable(map.get(x))
                                    .orElseThrow(s);
                        })
                .map(this::getPair)
                .distinct()
                .collect(Collectors.toList());
    }

    private Pair<SecurityScheme.Type, SecurityScheme.In> getPair(SecurityScheme securityScheme) {
        return new ImmutablePair<>(securityScheme.getType(), securityScheme.getIn());
    }

    protected Optional<ChangedSecurityRequirements> diff(
            List<SecurityRequirement> left, List<SecurityRequirement> right, DiffContext context) {
        left = left == null ? new ArrayList<>() : left;
        right = right == null ? new ArrayList<>() : getCopy(right);

        ChangedSecurityRequirements changedSecurityRequirements =
                new ChangedSecurityRequirements(left, right);

        for (SecurityRequirement leftSecurity : left) {
            Optional<SecurityRequirement> rightSecOpt = contains(right, leftSecurity);
            if (!rightSecOpt.isPresent()) {
                changedSecurityRequirements.addMissing(leftSecurity);
            } else {
                SecurityRequirement rightSec = rightSecOpt.get();
                right.remove(rightSec);
                Optional<ChangedSecurityRequirement> diff =
                        openApiDiff.getSecurityRequirementDiff().diff(leftSecurity, rightSec, context);
                diff.ifPresent(changedSecurityRequirements::addChanged);
            }
        }
        right.forEach(changedSecurityRequirements::addIncreased);

        return ChangedUtils.isChanged(changedSecurityRequirements);
    }

    private List<SecurityRequirement> getCopy(List<SecurityRequirement> right) {
        return right.stream().map(SecurityRequirementDiff::getCopy).collect(Collectors.toList());
    }
}
