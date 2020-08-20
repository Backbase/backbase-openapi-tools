package com.backbase.oss.boat.diff.compare;

import com.backbase.oss.boat.diff.model.ChangedMetadata;
import com.backbase.oss.boat.diff.model.DiffContext;
import com.backbase.oss.boat.diff.utils.ChangedUtils;
import io.swagger.v3.oas.models.Components;
import java.util.Optional;

@SuppressWarnings("FieldCanBeLocal")
public class MetadataDiff {

    private final OpenApiDiff openApiDiff;

    public MetadataDiff(OpenApiDiff openApiDiff) {
        this.openApiDiff = openApiDiff;
    }

    public Optional<ChangedMetadata> diff(String left, String right, DiffContext context) {
        return ChangedUtils.isChanged(new ChangedMetadata().setLeft(left).setRight(right));
    }
}
