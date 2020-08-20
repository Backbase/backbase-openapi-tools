package com.backbase.oss.boat.diff.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties({"summary","oldOperation","newOperation","extensions"})
public class ChangedOperation implements ComposedChanged {
  private Operation oldOperation;
  private Operation newOperation;

  private String pathUrl;
  private PathItem.HttpMethod httpMethod;
  @JsonIgnore
  private ChangedMetadata summary;
  @JsonIgnore
  private ChangedMetadata description;
  private boolean deprecated;
  private ChangedParameters parameters;
  private ChangedRequestBody requestBody;
  private ChangedApiResponse apiResponses;
  private ChangedSecurityRequirements securityRequirements;
  private ChangedExtensions extensions;

  public ChangedOperation(
      String pathUrl,
      PathItem.HttpMethod httpMethod,
      Operation oldOperation,
      Operation newOperation) {
    this.httpMethod = httpMethod;
    this.pathUrl = pathUrl;
    this.oldOperation = oldOperation;
    this.newOperation = newOperation;
  }

  @Override
  public List<Changed> getChangedElements() {
    return Arrays.asList(
        summary,
        description,
        parameters,
        requestBody,
        apiResponses,
        securityRequirements,
        extensions);
  }

  @Override
  public DiffResult isCoreChanged() {
    // TODO BETTER HANDLING FOR DEPRECIATION
    if (deprecated) {
      return DiffResult.COMPATIBLE;
    }
    return DiffResult.NO_CHANGES;
  }

  public DiffResult resultApiResponses() {
    return Changed.result(apiResponses);
  }

  public DiffResult resultRequestBody() {
    return requestBody == null ? DiffResult.NO_CHANGES : requestBody.isChanged();
  }
}
