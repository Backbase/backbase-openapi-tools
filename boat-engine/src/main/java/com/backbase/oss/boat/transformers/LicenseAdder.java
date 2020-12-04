package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LicenseAdder implements Transformer {

    public LicenseAdder(String licenseName, String licenseUrl) {
        this.licenseName = licenseName;
        this.licenseUrl = licenseUrl;
    }

    private final String licenseName;
    private final String licenseUrl;

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {
        if (openAPI.getInfo().getLicense() == null) {
            openAPI.getInfo().setLicense(new License().name(licenseName).url(licenseUrl));
            log.info("Adding License: {} with url: {} to Schema: {}", licenseName, licenseUrl, openAPI.getInfo().getTitle());
        }
    }
}
