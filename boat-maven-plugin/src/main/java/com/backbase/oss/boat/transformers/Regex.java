package com.backbase.oss.boat.transformers;

import lombok.NoArgsConstructor;
import org.codehaus.plexus.components.io.filemappers.RegExpFileMapper;

/**
 * Regex mapper, easy to configure in {@code pom.xml}
 */
@NoArgsConstructor
public class Regex extends RegExpFileMapper {

    public Regex(String pattern, String replacement) {
        setPattern(pattern);
        setReplacement(replacement);
    }
}
