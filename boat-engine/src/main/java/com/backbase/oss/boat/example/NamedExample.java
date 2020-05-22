package com.backbase.oss.boat.example;

import io.swagger.v3.oas.models.examples.Example;

import javax.validation.constraints.NotNull;

public class NamedExample {

    @NotNull
    private String name;
    @NotNull
    private Example example;

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
