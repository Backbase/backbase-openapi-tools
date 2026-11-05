package com.backbase.oss.boat.asyncapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Channel {

    /**
     * Key of the channel under {@code channels}.
     */
    private String key;

    /**
     * Address, possibly containing {@code {parameter}} expressions. {@code null} for dynamic addresses.
     */
    private String address;
    private String title;
    private String summary;
    private String description;
    private Map<String, Message> messages = new LinkedHashMap<>();
    private Map<String, ChannelParameter> parameters = new LinkedHashMap<>();
    private JsonNode bindings;

    public boolean isParameterized() {
        return address != null && address.contains("{");
    }
}
