package com.backbase.oss.boat.wharf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class BoatWharfApplication {

    public static void main(String[] args) {
        new SpringApplication(BoatWharfApplication.class).run(args);
    }
}
