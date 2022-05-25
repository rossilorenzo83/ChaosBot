package com.lr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Bean
    public ExecutorService getThreadPool(){
        return Executors.newFixedThreadPool(Config.WINDOWS_NAMES.size());
    }
}
