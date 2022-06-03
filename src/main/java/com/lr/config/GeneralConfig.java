package com.lr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix="general")
@Primary
@Configuration
@Getter
@Setter
public class GeneralConfig {
    static {
        System.setProperty("java.awt.headless", "false");
    }

    //Depends on client used
    private String pidName; //Warhammer Chaos

    private List<String> windowsNames;

    private Locale gameLanguage;

    public static Double SUPPORTED_IMG_WIDTH = 483D;
    public static Double SUPPORTED_IMG_HEIGHT = 835D;


}
