package it.francesco.synthesia.job.exception;

public class SynthesiaApiException extends RuntimeException {

    private final String apiResponseCode;

    public SynthesiaApiException(String message, String code) {
        super(message);
        this.apiResponseCode = code;
    }

    public String getApiResponseCode() {
        return apiResponseCode;
    }
}
