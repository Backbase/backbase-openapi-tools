package com.backbase.oss.boat.transformers;

import lombok.NoArgsConstructor;
import org.codehaus.plexus.components.io.filemappers.MergeFileMapper;

/**
 * Merge mapper, easy to configure in {@code pom.xml}
 */
@NoArgsConstructor
public class Merge extends MergeFileMapper {

    public Merge(String value) {
        set(value);
    }

    /**
     * Default setter, used at creation from POM configuration.
     */
    public void set(String value) {
        setTargetName(value);
    }
}
