package com.backbase.oss.boat.diff.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

public interface ComposedChanged extends Changed {

  @NotNull
  @JsonIgnore
  List<Changed> getChangedElements();

  @NotNull
  DiffResult isCoreChanged();

  @Override
  default DiffResult isChanged() {
    DiffResult elementsResult =
              DiffResult.fromWeight(
                      getChangedElements().stream()
                              .filter(Objects::nonNull)
                              .map(Changed::isChanged)
                              .mapToInt(DiffResult::getWeight)
                              .max()
                              .orElse(0));
      if (isCoreChanged().getWeight() > elementsResult.getWeight()) {
        return isCoreChanged();
      } else {
        return elementsResult;
    }
  }
}
