package com.backbase.oss.boat.radio;

import lombok.Data;

@Data
/**
 * Spec to be uploaded.
 */
public class SpecConfig {

    /**
     * Spec Key in Boat-Bay. Defaults to {@code inputSpecFile.getName().lastIndexOf("-")}.
     * For example - By default {@code my-service-api-v3.1.4.yaml} would be evaluated to {@code my-service-api}
     */
    private String key;

    /**
     * Spec Name in Boat-Bay. Defaults to filename.
     */
    private String name;

    /**
     * Location of the OpenAPI spec, as URL or local file glob pattern.
     * <p>
     * If the input is a local file, the value of this property is considered a glob pattern that must
     * resolve to a unique file.
     * </p>
     * <p>
     * The glob pattern allows to express the input specification in a version neutral way. For
     * instance, if the actual file is {@code my-service-api-v3.1.4.yaml} the expression could be
     * {@code my-service-api-v*.yaml}.
     * </p>
     */
    private String inputSpec;

}
