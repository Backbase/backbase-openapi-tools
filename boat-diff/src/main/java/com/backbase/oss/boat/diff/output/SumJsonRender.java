package com.backbase.oss.boat.diff.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.backbase.oss.boat.diff.model.ChangedOpenApiRenderList;

public class SumJsonRender {

    public String render(ChangedOpenApiRenderList changedOpenApiRenderList) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(changedOpenApiRenderList);
        return json;
    }
}
