package it.francesco.synthesia.job.service;

import it.francesco.synthesia.job.exception.SynthesiaApiException;
import it.francesco.synthesia.job.feign.SynthesiaClient;
import it.francesco.synthesia.job.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ListenerService {

    @Autowired
    public RabbitTemplate rabbitTemplate;

    @Autowired
    public SynthesiaClient synthesiaClient;

    public void sendSignatureToQueue(Message message) {
        log.info("Sending message in signatures queue: {}", message);
        rabbitTemplate.convertAndSend("signatures", message);
    }

    private String getSignatureFromApi(String message) {
        try {
            return synthesiaClient.getSignature(message);
        } catch (SynthesiaApiException ex) {
            log.info("API failed for message {} - error code {}", message, ex.getApiResponseCode());
            return null;
        }
    }

    @RabbitListener(containerFactory = "rabbitListenerContainerFactory", queues = "requests")
    public void requestsListener(Message inMessage) throws InterruptedException {
        log.info("Request received: " + inMessage);

        boolean hasDone = false;
        String signature = null;

        while(!hasDone){
            signature = getSignatureFromApi(inMessage.getMessage());
            if(signature != null) hasDone = true;
            else Thread.sleep(10000);
        }

        inMessage.setSignature(signature);

        sendSignatureToQueue(inMessage);

    }

}
