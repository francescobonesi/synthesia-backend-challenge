package it.francesco.synthesia.service;

import it.francesco.synthesia.model.Message;
import it.francesco.synthesia.model.MessageStatus;
import it.francesco.synthesia.model.Waiter;
import it.francesco.synthesia.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class SignatureServiceTest {

    private final RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
    private final MessageRepository messageRepository = Mockito.mock(MessageRepository.class);
    private final RabbitMqListener listener = Mockito.mock(RabbitMqListener.class);

    private SignatureService signatureService;

    @BeforeEach
    void setUp() {
        String sendQueue = "queue";
        Long timeout = 1L;
        signatureService = new SignatureService(
                rabbitTemplate, messageRepository, listener, sendQueue, timeout
        );
    }

    @Test
    void getMessageById_Present() {
        // given
        String identifier = "id";
        Message message = new Message();
        message.setIdentifier(identifier);

        // when
        Mockito.when(messageRepository.findById(Mockito.anyString()))
                .thenReturn(Optional.of(message));
        Optional<Message> returned = signatureService.getMessageById(identifier);

        // then
        assertTrue(returned.isPresent());
        assertNotNull(returned.get());
        assertEquals("id", returned.get().getIdentifier());

    }

    @Test
    void getMessageById_Absent() {
        // given
        String identifier = "id";

        // when
        Mockito.when(messageRepository.findById(Mockito.anyString()))
                .thenReturn(Optional.empty());
        Optional<Message> returned = signatureService.getMessageById(identifier);

        // then
        assertTrue(returned.isEmpty());

    }

    @Test
    void sign_NewIdentifier_WaitingTooMuch() {

        // given
        String identifier = "id";
        Message message = new Message();
        message.setIdentifier(identifier);
        final int[] counter = {0};
        Waiter waiter = new Waiter();
        waiter.setCountDownLatch(new CountDownLatch(1));
        waiter.setDone(false);

        // when
        Mockito.when(messageRepository.findById(Mockito.anyString()))
                .thenReturn(Optional.empty());
        Mockito.when(messageRepository.save(Mockito.any()))
                .thenReturn(message);
        Mockito.doAnswer((x) -> {
                    counter[0] += 1;
                    return null;
                }).when(rabbitTemplate)
                .convertAndSend(Mockito.anyString(), (Object) Mockito.any());
        Mockito.when(listener.addSignatureWaiter(Mockito.any())).thenReturn(waiter);
        Mockito.doNothing().when(listener).removeWaiter(Mockito.any(), Mockito.any());

        Message returned = signatureService.sign("sample", identifier);

        // then
        assertNull(returned);
        assertEquals(1, counter[0]);
    }


    @Test
    void sign_NewIdentifier_ReturnedImmediately() {

        // given
        String identifier = "id";
        String signature = "signature";
        Message message = new Message();
        message.setIdentifier(identifier);
        message.setSignature(signature);
        final int[] counter = {0};
        Waiter waiter = new Waiter();
        waiter.setCountDownLatch(new CountDownLatch(0));
        waiter.setDone(true);
        waiter.setMessage(message);

        // when
        Mockito.when(messageRepository.findById(Mockito.anyString()))
                .thenReturn(Optional.empty());
        Mockito.when(messageRepository.save(Mockito.any()))
                .thenReturn(message);
        Mockito.doAnswer((x) -> {
                    counter[0] += 1;
                    return null;
                }).when(rabbitTemplate)
                .convertAndSend(Mockito.anyString(), (Object) Mockito.any());
        Mockito.when(listener.addSignatureWaiter(Mockito.any())).thenReturn(waiter);
        Mockito.doNothing().when(listener).removeWaiter(Mockito.any(), Mockito.any());

        Message returned = signatureService.sign("sample", identifier);

        // then
        assertEquals(returned, message);
        assertEquals(1, counter[0]);
    }


    @Test
    void sign_AlreadyPresentIdentifier_InProgress_WaitingTooMuch() {

        // given
        String identifier = "id";
        String signature = "signature";
        Message message = new Message();
        message.setIdentifier(identifier);
        message.setSignature(signature);
        message.setStatus(MessageStatus.IN_PROGRESS);
        final int[] counter = {0};
        Waiter waiter = new Waiter();
        waiter.setCountDownLatch(new CountDownLatch(1));
        waiter.setDone(false);

        // when
        Mockito.when(messageRepository.findById(Mockito.anyString()))
                .thenReturn(Optional.of(message));
        Mockito.doAnswer((x) -> {
                    counter[0] += 1;
                    return null;
                }).when(rabbitTemplate)
                .convertAndSend(Mockito.anyString(), (Object) Mockito.any());
        Mockito.when(listener.addSignatureWaiter(Mockito.any())).thenReturn(waiter);
        Mockito.doNothing().when(listener).removeWaiter(Mockito.any(), Mockito.any());

        Message returned = signatureService.sign("sample", identifier);

        // then
        assertNull(returned);
        assertEquals(0, counter[0]);
    }


    @Test
    void sign_AlreadyPresentIdentifier_InProgress_ReturnedInTime() {

        // given
        String identifier = "id";
        String signature = "signature";
        Message message = new Message();
        message.setIdentifier(identifier);
        message.setSignature(signature);
        message.setStatus(MessageStatus.IN_PROGRESS);
        final int[] counter = {0};
        Waiter waiter = new Waiter();
        waiter.setCountDownLatch(new CountDownLatch(0));
        waiter.setDone(true);
        waiter.setMessage(message);

        // when
        Mockito.when(messageRepository.findById(Mockito.anyString()))
                .thenReturn(Optional.of(message));

        Mockito.doAnswer((x) -> {
                    counter[0] += 1;
                    return null;
                }).when(rabbitTemplate)
                .convertAndSend(Mockito.anyString(), (Object) Mockito.any());
        Mockito.when(listener.addSignatureWaiter(Mockito.any())).thenReturn(waiter);
        Mockito.doNothing().when(listener).removeWaiter(Mockito.any(), Mockito.any());

        Message returned = signatureService.sign("sample", identifier);

        // then
        assertEquals(returned, message);
        assertEquals(0, counter[0]);
    }

    @Test
    void sign_AlreadyPresentIdentifier_Signed_Returning() {

        // given
        String identifier = "id";
        String signature = "signature";
        Message message = new Message();
        message.setIdentifier(identifier);
        message.setSignature(signature);
        message.setStatus(MessageStatus.SIGNED);
        final int[] counter = {0};

        // when
        Mockito.when(messageRepository.findById(Mockito.anyString()))
                .thenReturn(Optional.of(message));
        Mockito.doAnswer((x) -> {
                    counter[0] += 1;
                    return null;
                }).when(rabbitTemplate)
                .convertAndSend(Mockito.anyString(), (Object) Mockito.any());

        Message returned = signatureService.sign("sample", identifier);

        // then
        assertEquals(returned, message);
        assertEquals(0, counter[0]);
    }


}