package com.lr.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix="discord")
@Setter
public class DiscordWebClientConfiguration {


    private String botAuthToken;
    private String channelId;

    @Bean
    public WebClient discordWebClient() {

        return WebClient
                .builder()
                .baseUrl("https://discord.com/api/v10/channels/"+channelId)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bot "+ botAuthToken)
                .build();
    }


}
