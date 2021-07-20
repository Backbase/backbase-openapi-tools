package com.backbase.oss.boat.transformers;

import lombok.NoArgsConstructor;
import org.codehaus.plexus.components.io.filemappers.PrefixFileMapper;

/**
 * Prefix mapper, easy to configure in {@code pom.xml}
 */
@NoArgsConstructor
public class Prefix extends PrefixFileMapper {

    public Prefix(String value) {
        set(value);
    }

    /**
     * Default setter, used at creation from POM configuration.
     */
    public void set(String value) {
        setPrefix(value);
    }
}
