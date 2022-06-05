package com.lr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix="farm")
@Configuration
@Setter
@Getter
public class MarchConfig {

    private Long marchesIntervalMins;

    private Integer marchesAvailable;

    private Integer targetLevel;

}
