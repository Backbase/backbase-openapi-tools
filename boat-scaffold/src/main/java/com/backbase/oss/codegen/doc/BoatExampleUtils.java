package com.backbase.oss.codegen.doc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class BoatExampleUtils {

  public static void convertExamples(MediaType mediaType, String contentType,  List<BoatExample> examples) {
      if (mediaType.getExample() != null) {
          Object example = mediaType.getExample();
          BoatExample boatExample = new BoatExample("example", contentType, new Example().value(example));
          if (example instanceof ObjectNode && ((ObjectNode) example).has("$ref")) {
              boatExample.getExample().set$ref(((ObjectNode) example).get("$ref").asText());
          }
          examples.add(boatExample);
      }
      if (mediaType.getExamples() != null) {
          mediaType.getExamples().forEach((key, example) -> {
              BoatExample boatExample = new BoatExample(key, contentType, example);
              examples.add(boatExample);
          });
      }
  }

  public static void inlineExamples(String name, List<BoatExample> examples, OpenAPI openAPI) {
      examples.stream()
          .filter(boatExample -> boatExample.getExample().get$ref() != null)
          .forEach(boatExample -> {
              String ref = boatExample.getExample().get$ref();
              if (ref.startsWith("#/components/examples")) {
                  ref = StringUtils.substringAfterLast(ref, "/");
                  if (openAPI.getComponents() != null && openAPI.getComponents().getExamples() != null) {
                      Example example = openAPI.getComponents().getExamples().get(ref);
                      if (example == null) {
                          log.warn("Example ref: {}  used in: {}  refers to an example that does not exist", ref, name);
                      } else {
                          log.debug("Replacing Example ref: {}  used in: {}  with example from components: {}", ref, name, example);
                          boatExample.setExample(example);
                      }
                  } else {
                      log.warn("Example ref: {}  used in: {}  refers to an example that does not exist", ref, name);
                  }
              } else {
                  log.warn("Example ref: {} used in: {} refers to an example that does not exist", ref, name);
              }
          });
  }

}
