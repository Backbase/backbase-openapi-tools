package com.backbase.oss.boat.diff.compare;

import com.backbase.oss.boat.diff.model.Change;
import com.backbase.oss.boat.diff.model.Changed;
import com.backbase.oss.boat.diff.model.DiffContext;

public interface ExtensionDiff {

    ExtensionDiff setOpenApiDiff(OpenApiDiff openApiDiff);

    String getName();

    Changed diff(Change extension, DiffContext context);

    default boolean isParentApplicable(
            Change.Type type, Object object, Object extension, DiffContext context) {
        return true;
    }
}
