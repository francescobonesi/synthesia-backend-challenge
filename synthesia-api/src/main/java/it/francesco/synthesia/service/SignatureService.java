package it.francesco.synthesia.service;

import it.francesco.synthesia.model.Message;
import it.francesco.synthesia.model.MessageStatus;
import it.francesco.synthesia.model.Waiter;
import it.francesco.synthesia.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

@Service
@Slf4j
public class SignatureService {

    public final RabbitTemplate rabbitTemplate;
    public final MessageRepository messageRepository;
    public final RabbitMqListener listener;
    public final String sendQueue;
    public final Long timeout;

    @Autowired
    public SignatureService(RabbitTemplate rabbitTemplate,
                            MessageRepository messageRepository,
                            RabbitMqListener listener,
                            @Value("${queue.requests}") String sendQueue,
                            @Value("${synthesia.timeout}") Long timeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageRepository = messageRepository;
        this.listener = listener;
        this.sendQueue = sendQueue;
        this.timeout = timeout;
    }

    public Optional<Message> getMessageById(String identifier) {
        return messageRepository.findById(identifier);
    }

    private void saveAndSendMessage(String identifier, String msgStr) {

        Message message = new Message();
        message.setIdentifier(identifier);
        message.setMessage(msgStr);
        message.setStatus(MessageStatus.IN_PROGRESS);

        try {

            log.info("Saving operation in progress in repository");
            messageRepository.save(message);

            log.info("Sending message in queue: {}", message);
            rabbitTemplate.convertAndSend(sendQueue, message);

        } catch (DataIntegrityViolationException e) {
            log.info("No need to worry. " +
                    "Means that while managing this request, " +
                    "the same request arrived and message has been already issued.");
        }

    }

    private Message getReturnedFromWaiter(String identifier, Waiter waiter) {
        if (waiter.isDone()) {
            log.info("Received in time signature for identifier {}", identifier);
            return waiter.getMessage();
        } else {
            log.info("Not received signature for identifier {}, removing waiter", identifier);
            return null;
        }
    }

    private Message waitSomeTimeFor(String identifier) {

        Waiter waiter = listener.addSignatureWaiter(identifier);
        try {
            boolean countdownHasDone = waiter.getCountDownLatch().await(timeout, SECONDS);
            log.info("Countdown for {} has ended with status {}", identifier, countdownHasDone);
        } catch (InterruptedException e) {
            log.warn("Got exception: {}", e.getMessage());
        }

        Message message = getReturnedFromWaiter(identifier, waiter);
        listener.removeWaiter(identifier, waiter);
        return message;


    }

    public Message sign(String msgStr, String identifier) {


        Optional<Message> savedSignature = getMessageById(identifier);

        if (savedSignature.isPresent() && savedSignature.get().getStatus().equals(MessageStatus.SIGNED)) {
            log.info("Found in cache, already present, identifier {}", identifier);
            return savedSignature.get();
        }

        if (savedSignature.isEmpty()) {
            log.info("This message has not been requested, identifier {}", identifier);
            saveAndSendMessage(identifier, msgStr);
        } else {
            log.info("Message already requested, just waiting identifier {}", identifier);
        }

        return waitSomeTimeFor(identifier);
    }
}
