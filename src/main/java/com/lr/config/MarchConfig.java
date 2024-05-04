package com.lr.config;

import com.lr.business.RssType;
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

    private String targetRssLevel;

    private String rssType;

    private String targetArmyLevel;
}
