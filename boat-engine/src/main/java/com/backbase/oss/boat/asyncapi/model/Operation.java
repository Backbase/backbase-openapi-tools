package com.backbase.oss.boat.asyncapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Operation {

    /**
     * Key of the operation under {@code operations}; used as the operation id.
     */
    private String key;
    private Action action;
    private Channel channel;

    /**
     * Messages of the operation. When the operation does not list any, all messages of its channel apply.
     */
    private List<Message> messages = new ArrayList<>();
    private Reply reply;
    private String title;
    private String summary;
    private String description;
    private List<String> tags = new ArrayList<>();
    private JsonNode bindings;

    public enum Action {
        SEND("send"),
        RECEIVE("receive");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public Action inverse() {
            return this == SEND ? RECEIVE : SEND;
        }

        public static Action fromValue(String value) {
            for (Action action : values()) {
                if (action.value.equals(value)) {
                    return action;
                }
            }
            throw new IllegalArgumentException("Unknown operation action '" + value + "', expected send or receive");
        }
    }

    @Data
    public static class Reply {

        /**
         * Reply channel; {@code null} when the reply address is only known at runtime.
         */
        private Channel channel;

        /**
         * Runtime expression for a dynamic reply address, e.g. {@code $message.header#/replyTo}.
         */
        private String addressLocation;
        private List<Message> messages = new ArrayList<>();
    }
}
