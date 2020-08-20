package com.backbase.oss.boat.diff.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Endpoint {

  private String pathUrl;
  private PathItem.HttpMethod method;
  private String summary;

  private PathItem path;
  private Operation operation;

  @Override
  public String toString() {
    return method + " " + pathUrl;
  }
}
