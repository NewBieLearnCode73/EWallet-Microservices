package com.dinhchieu.ewallet.common_library.config;

import org.apache.avro.specific.SpecificRecord;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Configuration
public class JacksonConfig {
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer addAvroMixin() {
    return builder -> builder.mixIn(SpecificRecord.class, AvroMixIn.class);
  }

  @JsonIgnoreProperties({ "schema", "specificData" })
  abstract class AvroMixIn {
  }
}
