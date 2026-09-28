package com.voiceflow;

import com.voiceflow.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class VoiceFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoiceFlowApplication.class, args);
    }
}
