package com.lr.config;

import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;

@Configuration
public class Config {
    static {
        System.setProperty("java.awt.headless", "false");
    }


    //Depends on client used
    public static String PID_NAME = "BlueStacks_nxt"; //Warhammer Chaos
    public static List<String> WINDOWS_NAMES = List.of("Deciphere", "Meph a new"/*,"Betafarm anew","Conquest"*/);
    public static Integer MARCH_AVAILABLE = 3;
    public static Locale GAME_LANGUAGE = Locale.FRENCH;
    //1h
    public static Long TIME_INTERVAL_MILLIS = 1000*60*60L;
}
