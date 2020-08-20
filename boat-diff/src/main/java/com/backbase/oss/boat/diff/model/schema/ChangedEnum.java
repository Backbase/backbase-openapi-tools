package com.backbase.oss.boat.diff.model.schema;

import com.backbase.oss.boat.diff.model.ChangedList;
import com.backbase.oss.boat.diff.model.DiffContext;
import com.backbase.oss.boat.diff.model.DiffResult;
import java.util.List;
import lombok.Getter;

@Getter
public class ChangedEnum<T> extends ChangedList<T> {

  public ChangedEnum(List<T> oldValue, List<T> newValue, DiffContext context) {
    super(oldValue, newValue, context);
  }

  @Override
  public DiffResult isItemsChanged() {
    if (context.isRequest() && getMissing().isEmpty()
        || context.isResponse() && getIncreased().isEmpty()) {
      return DiffResult.COMPATIBLE;
    }
    return DiffResult.INCOMPATIBLE;
  }
}
