package com.voiceflow.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.voiceflow.config.AppProperties;
import com.voiceflow.exception.ApiException;
import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Talks to any API that follows the OpenAI "audio/transcriptions" format
 * (multipart fields: file, model, language, response_format; JSON reply with a "text" field).
 * Endpoint, model and key come from configuration. Audio and transcripts are never logged.
 */
public class OpenAiCompatibleProvider implements SpeechToTextProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    private final AppProperties.Speech config;
    private final RestClient client;

    public OpenAiCompatibleProvider(AppProperties.Speech config, RestClient.Builder builder) {
        this.config = config;
        this.client = builder.build();
    }

    @Override
    public boolean isConfigured() {
        return config.hasApiKey();
    }

    @Override
    public TranscriptionResult transcribe(AudioPayload audio, String languageCode) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SPEECH_API_KEY_MISSING",
                    "No speech API key is configured. Set SPEECH_API_KEY (see README, step 'Configure the speech API') and restart the backend.");
        }

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(Objects.requireNonNull(audio.data())) {
            @Override
            public String getFilename() {
            return Objects.requireNonNull(audio.filename());
            }
        });
        form.add("model", config.model());
        form.add("response_format", "json");
        if (languageCode != null && !languageCode.isBlank()) {
            form.add("language", languageCode);
        }

        try {
            JsonNode reply = client.post()
                    .uri(Objects.requireNonNull(config.endpoint()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
                    .contentType(Objects.requireNonNull(MediaType.MULTIPART_FORM_DATA))
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            if (reply == null || !reply.hasNonNull("text") || !reply.get("text").isTextual()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "TRANSCRIPTION_PROVIDER_ERROR",
                        "The speech provider sent a response we could not understand.");
            }
            return new TranscriptionResult(Objects.requireNonNull(reply.get("text").asText()).trim(), languageCode);

        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            log.warn("Speech provider returned HTTP {}", status);
            throw mapStatus(status);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "TRANSCRIPTION_TIMEOUT",
                        "The speech provider took too long to respond. Try a shorter recording or try again.");
            }
            log.warn("Could not reach speech provider: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "TRANSCRIPTION_PROVIDER_ERROR",
                    "The backend could not reach the speech provider. Check the internet connection and SPEECH_API_ENDPOINT.");
        } catch (RestClientException e) {
            // Includes replies that were not valid JSON.
            log.warn("Speech provider reply could not be processed: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "TRANSCRIPTION_PROVIDER_ERROR",
                    "The speech provider sent a response we could not understand.");
        }
    }

    private ApiException mapStatus(int status) {
        return switch (status) {
            case 401, 403 -> new ApiException(HttpStatus.BAD_GATEWAY, "SPEECH_API_AUTH_FAILED",
                    "The speech provider rejected the API key. Check SPEECH_API_KEY.");
            case 429 -> new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SPEECH_API_RATE_LIMIT",
                    "The speech provider's rate limit or quota was reached. Wait a moment or check your plan.");
            case 404 -> new ApiException(HttpStatus.BAD_GATEWAY, "TRANSCRIPTION_PROVIDER_ERROR",
                    "The speech provider could not find the endpoint or model. Check SPEECH_API_ENDPOINT and SPEECH_MODEL.");
            case 400, 413, 415, 422 -> new ApiException(HttpStatus.BAD_GATEWAY, "TRANSCRIPTION_PROVIDER_ERROR",
                    "The speech provider could not process this audio. Try recording again.");
            default -> new ApiException(HttpStatus.BAD_GATEWAY, "TRANSCRIPTION_PROVIDER_ERROR",
                    "Transcription could not be completed. Please try again.");
        };
    }

    private static boolean isTimeout(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof java.net.SocketTimeoutException
                    || c instanceof HttpTimeoutException
                    || c instanceof InterruptedIOException) {
                return true;
            }
            if (c.getCause() == c) break;
        }
        return false;
    }
}
