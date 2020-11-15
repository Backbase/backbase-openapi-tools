package com.backbase.oss.boat.transformers;

import java.util.Map;

import static java.util.Optional.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpecVersionTransformer implements Transformer {

    @NonNull
    private final String version;

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {
        final Info info = ofNullable(openAPI.getInfo()).orElseGet(Info::new);

        info.setVersion(this.version);

        openAPI.setInfo(info);
    }

}


