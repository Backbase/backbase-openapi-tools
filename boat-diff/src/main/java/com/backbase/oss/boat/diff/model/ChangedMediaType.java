package com.backbase.oss.boat.diff.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties({"oldSchema", "newSchema","context"})
public class ChangedMediaType implements ComposedChanged {
  private final Schema oldSchema;
  private final Schema newSchema;
  private final DiffContext context;
  private ChangedSchema schema;

  public ChangedMediaType(Schema oldSchema, Schema newSchema, DiffContext context) {
    this.oldSchema = oldSchema;
    this.newSchema = newSchema;
    this.context = context;
  }

  @Override
  public List<Changed> getChangedElements() {
    return Collections.singletonList(schema);
  }

  @Override
  public DiffResult isCoreChanged() {
    return DiffResult.NO_CHANGES;
  }
}
