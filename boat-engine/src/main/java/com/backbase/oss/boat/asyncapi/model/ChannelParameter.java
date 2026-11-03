package com.backbase.oss.boat.asyncapi.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ChannelParameter {

    private String name;
    private String description;
    private List<String> enumValues = new ArrayList<>();
    private String defaultValue;
    private List<String> examples = new ArrayList<>();

    /**
     * Runtime expression, e.g. {@code $message.payload#/user/id}.
     */
    private String location;
}
