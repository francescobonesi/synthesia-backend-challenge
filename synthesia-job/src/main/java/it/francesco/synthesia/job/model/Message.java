package it.francesco.synthesia.job.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@ToString
public class Message {

    private String identifier;
    private String message;
    private String signature;

}
