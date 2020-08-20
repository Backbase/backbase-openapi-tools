package com.backbase.oss.boat.diff.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.backbase.oss.boat.diff.model.ChangedOpenApi;

public interface Render {

    String render(ChangedOpenApi diff) throws JsonProcessingException;
}
