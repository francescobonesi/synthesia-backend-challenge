package it.francesco.synthesia.service;

import it.francesco.synthesia.model.Message;
import it.francesco.synthesia.model.MessageStatus;
import it.francesco.synthesia.model.Waiter;
import it.francesco.synthesia.repository.MessageRepository;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@Service
@Slf4j
public class RabbitMqListener {

    Map<String, List<Waiter>> signatureWaiters;
    public final MessageRepository messageRepository;

    @Autowired
    public RabbitMqListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
        this.signatureWaiters = new HashMap<>();
    }

    @Synchronized
    public Waiter addSignatureWaiter(String identifier) {
        Waiter waiter = new Waiter();
        waiter.setCountDownLatch(new CountDownLatch(1));

        if (signatureWaiters.containsKey(identifier)) {
            signatureWaiters.get(identifier).add(waiter);
        } else {
            ArrayList<Waiter> waiters = new ArrayList<>();
            waiters.add(waiter);
            signatureWaiters.put(identifier, waiters);
        }

        return waiter;

    }

    @Synchronized
    public void removeWaiter(String identifier, Waiter waiter) {
        List<Waiter> waiters = signatureWaiters.get(identifier);
        if (CollectionUtils.isEmpty(waiters)) {
            log.warn("There are no waiters for identifier {}", identifier);
            return;
        }

        boolean removed = waiters.remove(waiter);
        log.info("Waiter has been removed with status: {}", removed);

        int waiterSizeForIdentifier = waiters.size();
        if (waiterSizeForIdentifier == 0) {
            log.info("Waiters for identifier {} are zero, removing from map", identifier);
            signatureWaiters.remove(identifier);
        } else {
            log.info("Waiters for identifier {} are still {}", identifier, waiterSizeForIdentifier);
        }
    }

    @RabbitListener(queues = "${queue.signatures}")
    public void signaturesListener(Message inMessage) {
        log.info("Message received from signature job: " + inMessage);
        inMessage.setStatus(MessageStatus.SIGNED);
        messageRepository.save(inMessage);

        List<Waiter> waiters = signatureWaiters.getOrDefault(inMessage.getIdentifier(), Collections.emptyList());
        log.info("Closing waiters for identifier {} (requests {})", inMessage.getIdentifier(), waiters.size());
        for (Waiter waiter : waiters) {
            waiter.setMessage(inMessage);
            waiter.setDone(true);
            waiter.getCountDownLatch().countDown();
        }
    }

}
