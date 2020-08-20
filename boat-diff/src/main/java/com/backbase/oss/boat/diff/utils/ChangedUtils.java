package com.backbase.oss.boat.diff.utils;

import com.backbase.oss.boat.diff.model.Changed;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ChangedUtils {

  public static boolean isUnchanged(Changed changed) {
    return changed == null || changed.isUnchanged();
  }

  public static boolean isCompatible(Changed changed) {
    return changed == null || changed.isCompatible();
  }

  public static <T extends Changed> Optional<T> isChanged(T changed) {
    if (isUnchanged(changed)) {
      return Optional.empty();
    }
    return Optional.of(changed);
  }
}
