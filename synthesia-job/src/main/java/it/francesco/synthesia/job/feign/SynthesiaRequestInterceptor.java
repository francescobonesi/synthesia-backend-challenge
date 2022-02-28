package it.francesco.synthesia.job.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class SynthesiaRequestInterceptor implements RequestInterceptor {

    public String apiKey;

    public SynthesiaRequestInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", apiKey);
    }
}
