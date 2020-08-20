package com.backbase.oss.boat.diff.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Created by adarsh.sharma on 27/12/17. */
@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"oldRequestBody", "newRequestBody","context","changeRequired"})
public class ChangedRequestBody implements ComposedChanged {
  private final RequestBody oldRequestBody;
  private final RequestBody newRequestBody;
  private final DiffContext context;

  private boolean changeRequired;
  private ChangedMetadata description;
  private ChangedContent content;
  private ChangedExtensions extensions;

  public ChangedRequestBody(
      RequestBody oldRequestBody, RequestBody newRequestBody, DiffContext context) {
    this.oldRequestBody = oldRequestBody;
    this.newRequestBody = newRequestBody;
    this.context = context;
  }

  @Override
  public List<Changed> getChangedElements() {
    return Arrays.asList(description, content, extensions);
  }

  @Override
  public DiffResult isCoreChanged() {
    if (!changeRequired) {
      return DiffResult.NO_CHANGES;
    }
    return DiffResult.INCOMPATIBLE;
  }
}
