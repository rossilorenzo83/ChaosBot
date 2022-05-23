package com.lr.config;

import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Config {
    static {
        System.setProperty("java.awt.headless", "false");
    }

    public static String BLUESTACK_PID_NAME = "BlueStacks_nxt";
    public static List<String> WINDOWS_NAMES = List.of("Deciphere"/*, "Betafarm anew", "Meph a new"*/);
    public static Integer MARCH_AVAILABLE = 2;
    //1h
    public static Long TIME_INTERVAL_MILLIS = 1000*60*60L;
}
