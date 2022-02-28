package it.francesco.synthesia.job.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import it.francesco.synthesia.job.exception.SynthesiaApiException;
import org.springframework.http.HttpStatus;

public class SynthesiaErrorDecoder implements ErrorDecoder {

    public static final String ERROR_RESPONSE_CODE = "Error response code";

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus responseStatus = HttpStatus.valueOf(response.status());

        if (responseStatus.is5xxServerError()) {
            return new SynthesiaApiException(ERROR_RESPONSE_CODE, responseStatus.toString());
        } else if (responseStatus.is4xxClientError()) {
            return new SynthesiaApiException(ERROR_RESPONSE_CODE, responseStatus.toString());
        } else {
            return new Exception(ERROR_RESPONSE_CODE);
        }
    }
}