package com.backbase.oss.codegen.yard.model;

import java.util.List;
import lombok.Data;

@Data
public class Capability {

    private String key;
    private String title;
    private String subTitle;
    private String navTitle;
    private String content;
    private String version;

    private List<Service> services;
}
