package com.lr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.*;
import java.util.Random;

@Configuration
public class Beans {

    @Bean
    public Robot sharedRobot() throws AWTException {
        return new Robot();
    };

    @Bean
    public Random sharedRandom() {
        return new Random();
    }

}
