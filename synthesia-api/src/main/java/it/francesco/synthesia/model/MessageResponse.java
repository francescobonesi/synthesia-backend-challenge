package it.francesco.synthesia.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class MessageResponse {

    private String signature;
    private String info;
    private String waitingWebsite;
    private String pollingPath;

}
