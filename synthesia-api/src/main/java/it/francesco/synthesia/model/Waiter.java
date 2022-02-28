package it.francesco.synthesia.model;

import lombok.Data;

import java.util.concurrent.CountDownLatch;

@Data
public class Waiter {

    private boolean done = false;
    private Message message;
    private CountDownLatch countDownLatch;

}
