package com.backbase.oss.boat.transformers;

import org.codehaus.plexus.components.io.filemappers.SuffixFileMapper;

/**
 * Suffix mapper, easy to configure in {@code pom.xml}
 */
public class Suffix extends SuffixFileMapper {

    public Suffix(String value) {
        set(value);
    }

    /**
     * Default setter, used at creation from POM configuration.
     */
    public void set(String value) {
        setSuffix(value);
    }
}
