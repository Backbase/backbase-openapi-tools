package com.backbase.oss.boat.transformers;

import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import static java.util.Optional.ofNullable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SetVersion implements Transformer {

    @NonNull
    private String version;

    @Override
    public OpenAPI transform(OpenAPI openAPI, Map<String, Object> options) {
        final Info info = ofNullable(openAPI.getInfo()).orElseGet(Info::new);

        info.setVersion(this.version);

        openAPI.setInfo(info);

        return openAPI;
    }

    /**
     * Default setter, used at creation from POM configuration.
     */
    public void set(String version) {
        setVersion(version);
    }

}

