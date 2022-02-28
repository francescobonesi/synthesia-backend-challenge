package it.francesco.synthesia.job.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Message {

    private String identifier;
    private String message;
    private String signature;

}
