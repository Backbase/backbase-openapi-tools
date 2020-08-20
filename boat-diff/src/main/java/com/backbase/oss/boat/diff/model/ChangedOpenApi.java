package com.backbase.oss.boat.diff.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.backbase.oss.boat.diff.utils.EndpointUtils;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Created by adarsh.sharma on 22/12/17. */
@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"oldSpecOpenApi", "newSpecOpenApi","oldSpecOpenApi","newSpecOpenApi"})
public class ChangedOpenApi implements ComposedChanged {

  private OpenAPI oldSpecOpenApi;
  private OpenAPI newSpecOpenApi;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<Endpoint> newEndpoints;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<Endpoint> missingEndpoints;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<ChangedOperation> changedOperations;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ChangedExtensions changedExtensions;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public List<Endpoint> getDeprecatedEndpoints() {
    return changedOperations.stream()
        .filter(ChangedOperation::isDeprecated)
        .map(
            c ->
                EndpointUtils.convert2Endpoint(
                    c.getPathUrl(), c.getHttpMethod(), c.getNewOperation()))
        .collect(Collectors.toList());
  }

  @Override
  public List<Changed> getChangedElements() {
    return Stream.concat(changedOperations.stream(), Stream.of(changedExtensions))
        .collect(Collectors.toList());
  }

  @Override
  public DiffResult isCoreChanged() {
    if (newEndpoints.isEmpty() && missingEndpoints.isEmpty()) {
      return DiffResult.NO_CHANGES;
    }
    if (missingEndpoints.isEmpty()) {
      return DiffResult.COMPATIBLE;
    }
    return DiffResult.INCOMPATIBLE;
  }
}
