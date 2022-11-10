package com.backbase.oss.boat.example;

import io.swagger.v3.oas.models.examples.Example;
import jakarta.validation.constraints.NotNull;


/**
 * A data class that has an example and its name.
 * The name is necessary for the json file name of the exploded inline examples.
 */
public class NamedExample {

    @NotNull
    private String name;
    @NotNull
    private Example example;

    /**
     * Constructs a new instance of this data class.
     *
     * @param name    the name of the example. No assumptions on the case are made at this point.
     * @param example the example.
     */
    public NamedExample(@NotNull String name, @NotNull Example example) {
        this.name = name;
        this.example = example;
    }

    public String getName() {
        return name;
    }

    public Example getExample() {
        return example;
    }

    public NamedExample name(String name) {
        this.name = name;
        return this;
    }

    public NamedExample example(Example example) {
        this.example = example;
        return this;
    }
}
