package it.francesco.synthesia.job.configuration;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import it.francesco.synthesia.job.feign.SynthesiaErrorDecoder;
import it.francesco.synthesia.job.feign.SynthesiaRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor(@Value("${synthesia.key}") String apiKey) {
        return new SynthesiaRequestInterceptor(apiKey);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new SynthesiaErrorDecoder();
    }

}
