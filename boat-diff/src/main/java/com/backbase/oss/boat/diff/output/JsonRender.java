package com.backbase.oss.boat.diff.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.backbase.oss.boat.diff.model.ChangedOpenApi;


public class JsonRender implements Render {
    protected ChangedOpenApi diff;

    @Override
    public String render(ChangedOpenApi diff) throws JsonProcessingException {
        this.diff = diff;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter().withoutAttribute("changedElements");

        String json = ow.writeValueAsString(diff);
        return json;
    }

}
