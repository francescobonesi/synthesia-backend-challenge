package it.francesco.synthesia.job.service;

import it.francesco.synthesia.job.exception.ListenerException;
import it.francesco.synthesia.job.exception.SynthesiaApiException;
import it.francesco.synthesia.job.feign.SynthesiaClient;
import it.francesco.synthesia.job.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListenerServiceTest {

    final RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
    final SynthesiaClient synthesiaClient = Mockito.mock(SynthesiaClient.class);
    final String sendQueue = "queue";
    final Long retryTime = 100L;
    final Long timeout = 2000L;

    ListenerService listenerService;
    Message message;


    @BeforeEach
    void setUp() {
        this.listenerService = new ListenerService(rabbitTemplate, synthesiaClient, sendQueue, retryTime, timeout);

    }

    @Test
    void requestsListener_firstCallSuccess() throws InterruptedException {
        // given
        message = new Message();
        message.setIdentifier("sample");
        message.setMessage("sample");
        String signature = "signature";

        // when
        Mockito.when(synthesiaClient.getSignature(Mockito.anyString())).thenReturn(signature);
        Mockito.when(synthesiaClient.verifySignature(Mockito.anyString(), Mockito.anyString())).thenReturn("ok");
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), (Object) Mockito.any());

        listenerService.requestsListener(message);

        // then
        assertEquals(signature, message.getSignature());

    }

    @Test
    void requestsListener_severalCallBeforeSuccess() throws InterruptedException {
        // given
        message = new Message();
        message.setIdentifier("sample");
        message.setMessage("sample");
        String signature = "signature";
        final int[] counter = {0};

        // when
        Mockito.doAnswer((action) -> {
            if (counter[0] < 10) {
                counter[0]++;
                throw new SynthesiaApiException("counter=" + counter[0], "500");
            } else return signature;

        }).when(synthesiaClient).getSignature(Mockito.anyString());
        Mockito.when(synthesiaClient.verifySignature(Mockito.anyString(), Mockito.anyString())).thenReturn("ok");
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), (Object) Mockito.any());

        listenerService.requestsListener(message);

        // then
        assertEquals(signature, message.getSignature());
        assertEquals(10, counter[0]);

    }


    @Test
    void requestsListener_verificationFailsFirstTimeButSecondTimeOk() throws InterruptedException {
        // given
        message = new Message();
        message.setIdentifier("sample");
        message.setMessage("sample");
        String signature = "signature";
        final int[] counter = {0};

        // when
        Mockito.when(synthesiaClient.getSignature(Mockito.anyString())).thenReturn(signature);
        Mockito.doAnswer((action) -> {
            if (counter[0] < 1) {
                counter[0]++;
                throw new SynthesiaApiException("counter=" + counter[0], "400");
            } else return "ok";

        }).when(synthesiaClient).verifySignature(Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), (Object) Mockito.any());

        listenerService.requestsListener(message);

        // then
        assertEquals(signature, message.getSignature());

    }


    @Test
    void requestsListener_exceedTimeout() {
        // given
        message = new Message();
        message.setIdentifier("sample");
        message.setMessage("sample");

        // when
        Mockito.doAnswer((action) -> {
            throw new SynthesiaApiException("NO", "500");
        }).when(synthesiaClient).getSignature(Mockito.anyString());
        Mockito.when(synthesiaClient.verifySignature(Mockito.anyString(), Mockito.anyString())).thenReturn("ok");
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(Mockito.any(), (Object) Mockito.any());

        // then
        assertThrows(ListenerException.class, () -> listenerService.requestsListener(message));

    }

}