package com.voiceflow.config;

import com.voiceflow.provider.OpenAiCompatibleProvider;
import com.voiceflow.provider.SpeechToTextProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Chooses which speech provider is used. To add another provider, create a new bean here. */
@Configuration
public class SpeechConfig {

    @Bean
    public SpeechToTextProvider speechToTextProvider(AppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.speech().timeout());
        factory.setReadTimeout(props.speech().timeout());
        return new OpenAiCompatibleProvider(props.speech(), RestClient.builder().requestFactory(factory));
    }
}
