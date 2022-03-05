package it.francesco.synthesia.job.service;

import it.francesco.synthesia.job.exception.ListenerException;
import it.francesco.synthesia.job.exception.SynthesiaApiException;
import it.francesco.synthesia.job.feign.SynthesiaClient;
import it.francesco.synthesia.job.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ListenerService {

    public final RabbitTemplate rabbitTemplate;
    public final SynthesiaClient synthesiaClient;
    public final String sendQueue;
    public final Long retryTime;
    public final Long timeout;

    @Autowired
    public ListenerService(RabbitTemplate rabbitTemplate,
                           SynthesiaClient synthesiaClient,
                           @Value("${queue.signatures}") String sendQueue,
                           @Value("${synthesia.retryTime}") Long retryTime,
                           @Value("${synthesia.timeout}") Long timeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.synthesiaClient = synthesiaClient;
        this.sendQueue = sendQueue;
        this.retryTime = retryTime;
        this.timeout = timeout;
    }

    private void sendSignatureToQueue(Message message) {
        log.info("Sending message in signatures queue: {}", message);
        rabbitTemplate.convertAndSend(sendQueue, message);
    }

    private String getSignatureFromApi(String message) {
        try {
            return synthesiaClient.getSignature(message);
        } catch (SynthesiaApiException ex) {
            log.info("GetSignature failed for message={}, responseCode={}", message, ex.getApiResponseCode());
            return null;
        }
    }

    private boolean verifySignature(String message, String signature) {
        try {
            // if api returns http 200 is fine, so just checking that response body has something in it
            String verifyResponseBody = synthesiaClient.verifySignature(message, signature);
            log.info("Verify API for message={} returned body={}", message, verifyResponseBody);
            return StringUtils.hasLength(verifyResponseBody);
        } catch (SynthesiaApiException ex) {
            log.info("Verification of signature failed for message={}, responseCode={}", message, ex.getApiResponseCode());
            return false;
        }
    }

    @RabbitListener(containerFactory = "rabbitListenerContainerFactory",
            queues = "${queue.requests}")
    public void requestsListener(Message inMessage) throws InterruptedException {

        log.info("Request received: " + inMessage);
        String message = inMessage.getMessage();
        boolean hasDone = false;
        String signature = null;
        long startTime = System.currentTimeMillis();

        // checking if signature arrived and if timeout is not exceeded
        while (!hasDone && System.currentTimeMillis() - startTime < timeout) {
            signature = getSignatureFromApi(message);
            if (signature != null) {
                hasDone = verifySignature(message, signature);
            } else Thread.sleep(retryTime);
        }

        if (!hasDone) {
            // means that timeout expired
            throw new ListenerException(
                    String.format("timeout of %s expired when signing message %s, not sending ACK to rabbit",
                            timeout, message)
            );
        }

        inMessage.setSignature(signature);
        log.info("Completed for identifier = {}", inMessage.getIdentifier());
        sendSignatureToQueue(inMessage);

    }

}
