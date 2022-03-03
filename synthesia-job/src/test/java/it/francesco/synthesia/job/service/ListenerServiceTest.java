package it.francesco.synthesia.job.service;

import it.francesco.synthesia.job.exception.SynthesiaApiException;
import it.francesco.synthesia.job.feign.SynthesiaClient;
import it.francesco.synthesia.job.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;

class ListenerServiceTest {

    final RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
    final SynthesiaClient synthesiaClient = Mockito.mock(SynthesiaClient.class);
    final String sendQueue = "queue";
    final Long retryTime = 100L;

    ListenerService listenerService;
    Message message;

    @BeforeEach
    void setUp() {
        this.listenerService = new ListenerService(rabbitTemplate, synthesiaClient, sendQueue, retryTime);

    }

    @Test
    void requestsListener_firstCallSuccess() throws InterruptedException{
        // given
        message = new Message();
        message.setIdentifier("sample");
        message.setMessage("sample");
        String signature = "signature";

        // when
        Mockito.when(synthesiaClient.getSignature(Mockito.anyString())).thenReturn(signature);
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), (Object) Mockito.any());

        listenerService.requestsListener(message);

        // then
        assertEquals(signature, message.getSignature());

    }

    @Test
    void requestsListener_severalCallBeforeSuccess() throws InterruptedException{
        // given
        message = new Message();
        message.setIdentifier("sample");
        message.setMessage("sample");
        String signature = "signature";
        final int[] counter = {0};

        // when
        Mockito.doAnswer((action) -> {
            if(counter[0] < 10) {
                counter[0]++;
                throw new SynthesiaApiException("counter=" + counter[0], "500");
            }
            else return signature;

        }).when(synthesiaClient).getSignature(Mockito.anyString());
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), (Object) Mockito.any());

        listenerService.requestsListener(message);

        // then
        assertEquals(signature, message.getSignature());
        assertEquals(10, counter[0]);

    }

}