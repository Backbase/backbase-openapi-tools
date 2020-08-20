package com.backbase.oss.boat.diff.compare;

import com.backbase.oss.boat.diff.model.ChangedContent;
import com.backbase.oss.boat.diff.model.ChangedMediaType;
import com.backbase.oss.boat.diff.model.DiffContext;
import com.backbase.oss.boat.diff.utils.ChangedUtils;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContentDiff implements Comparable<Content> {

    private OpenApiDiff openApiDiff;

    public ContentDiff(OpenApiDiff openApiDiff) {
        this.openApiDiff = openApiDiff;
    }

    @Override
    public boolean compare(Content left, Content right) {
        return false;
    }

    public Optional<ChangedContent> diff(Content left, Content right, DiffContext context) {

        MapKeyDiff<String, MediaType> mediaTypeDiff = MapKeyDiff.diff(left, right);
        List<String> sharedMediaTypes = mediaTypeDiff.getSharedKey();
        Map<String, ChangedMediaType> changedMediaTypes = new LinkedHashMap<>();
        for (String mediaTypeKey : sharedMediaTypes) {
            MediaType oldMediaType = left.get(mediaTypeKey);
            MediaType newMediaType = right.get(mediaTypeKey);
            ChangedMediaType changedMediaType =
                    new ChangedMediaType(oldMediaType.getSchema(), newMediaType.getSchema(), context);
            openApiDiff
                    .getSchemaDiff()
                    .diff(
                            new HashSet<>(),
                            oldMediaType.getSchema(),
                            newMediaType.getSchema(),
                            context.copyWithRequired(true))
                    .ifPresent(changedMediaType::setSchema);
            if (!ChangedUtils.isUnchanged(changedMediaType)) {
                changedMediaTypes.put(mediaTypeKey, changedMediaType);
            }
        }
        return ChangedUtils.isChanged(
                new ChangedContent(left, right, context)
                        .setIncreased(mediaTypeDiff.getIncreased())
                        .setMissing(mediaTypeDiff.getMissing())
                        .setChanged(changedMediaTypes));
    }
}
