package it.francesco.synthesia.service;

import it.francesco.synthesia.model.Message;
import it.francesco.synthesia.model.MessageStatus;
import it.francesco.synthesia.model.Waiter;
import it.francesco.synthesia.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class RabbitServiceTest {

    final MessageRepository messageRepository = Mockito.mock(MessageRepository.class);
    Message message;
    String identifier = "identifier";
    HashMap<String, List<Waiter>> waiters;

    RabbitMqListener listener;

    @BeforeEach
    void setUp() {
        listener = new RabbitMqListener(messageRepository);
        waiters = new HashMap<>();

        message = new Message();
        message.setMessage("prova");
        message.setIdentifier(identifier);
        Mockito.when(messageRepository.save(Mockito.any()))
                .thenReturn(message);

    }

    @Test
    void signaturesListener_allOk_NoWaiters() {
        // given
        assertNull(message.getStatus());

        // when
        listener.signaturesListener(message);

        // then
        assertEquals(MessageStatus.SIGNED, message.getStatus());

    }

    private Waiter generateWaiter() {
        Waiter w = new Waiter();
        w.setCountDownLatch(new CountDownLatch(1));
        w.setDone(false);
        w.setMessage(message);
        return w;
    }

    private List<Waiter> generateWaiterList(int size) {
        List<Waiter> wl = new LinkedList<>();
        for (int i = 0; i < size; i++) wl.add(generateWaiter());
        return wl;
    }

    @Test
    void signaturesListener_allOk_OneWaiter() {

        // given
        assertNull(message.getStatus());
        waiters.put(identifier, generateWaiterList(1));
        listener.signatureWaiters = waiters;

        // when
        listener.signaturesListener(message);

        // then
        assertEquals(MessageStatus.SIGNED, message.getStatus());
        Waiter waiter = waiters.get(identifier).get(0);
        assertNotNull(waiter);
        assertTrue(waiter.isDone());

    }


    @Test
    void signaturesListener_allOk_SeveralWaiter() {

        // given
        assertNull(message.getStatus());
        waiters.put(identifier, generateWaiterList(10));
        listener.signatureWaiters = waiters;

        // when
        listener.signaturesListener(message);

        // then
        assertEquals(MessageStatus.SIGNED, message.getStatus());
        for ( int j = 0; j < 10; j++){
            Waiter waiter = waiters.get(identifier).get(j);
            assertNotNull(waiter);
            assertTrue(waiter.isDone());
        }


    }

    @Test
    void addSignatureWaiter_ok_keyEmpty() {

        // given
        listener.signatureWaiters = waiters;
        assertNull(listener.signatureWaiters.get(identifier));

        // when
        Waiter waiter = listener.addSignatureWaiter(identifier);

        // then
        assertFalse(waiter.isDone());
        assertEquals(1, listener.signatureWaiters.get(identifier).size());

    }

    @Test
    void addSignatureWaiter_ok_keyAlreadyPresent() {

        // given
        listener.signatureWaiters = waiters;
        listener.signatureWaiters.put(identifier, generateWaiterList(3));
        assertEquals(3, listener.signatureWaiters.get(identifier).size());

        // when
        Waiter waiter = listener.addSignatureWaiter(identifier);

        // then
        assertFalse(waiter.isDone());
        assertEquals(4, listener.signatureWaiters.get(identifier).size());

    }

    @Test
    void removeWaiter_noKey() {
        // given
        Waiter waiter = generateWaiter();
        assertNull(listener.signatureWaiters.get(identifier));

        // when
        listener.removeWaiter(identifier, waiter);

        // then
        assertNull(listener.signatureWaiters.get(identifier));

    }

    @Test
    void removeWaiter_severalWaitersInList() {
        // given
        Waiter waiter1 = generateWaiter();
        Waiter waiter2 = generateWaiter();
        Waiter waiter3 = generateWaiter();
        List<Waiter> waiterList = new LinkedList<>();
        waiterList.add(waiter1);
        waiterList.add(waiter2);
        waiterList.add(waiter3);
        waiters.put(identifier, waiterList);
        listener.signatureWaiters = waiters;

        // when
        listener.removeWaiter(identifier, waiter1);

        // then
        assertEquals(2, listener.signatureWaiters.get(identifier).size());

        // and when
        listener.removeWaiter(identifier, waiter2);

        // then
        assertEquals(1, listener.signatureWaiters.get(identifier).size());

    }

    @Test
    void removeWaiter_lastWaitersInList() {
        // given
        Waiter waiter = generateWaiter();
        List<Waiter> waiterList = new LinkedList<>();
        waiterList.add(waiter);
        waiters.put(identifier, waiterList);
        listener.signatureWaiters = waiters;

        // when
        listener.removeWaiter(identifier, waiter);

        // then
        assertNull(listener.signatureWaiters.get(identifier));

    }

}