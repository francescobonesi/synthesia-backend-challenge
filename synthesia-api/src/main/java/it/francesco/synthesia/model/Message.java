package it.francesco.synthesia.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@ToString
@Table(name = "message")
@Entity
public class Message {

    @Id
    private String identifier;
    @Column(length = 1000)
    private String message;
    @Column(length = 1000)
    private String signature;
    private MessageStatus status;

}
