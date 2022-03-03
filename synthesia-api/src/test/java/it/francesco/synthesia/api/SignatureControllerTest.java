package it.francesco.synthesia.api;

import it.francesco.synthesia.Utils;
import it.francesco.synthesia.model.Message;
import it.francesco.synthesia.model.MessageResponse;
import it.francesco.synthesia.model.MessageStatus;
import it.francesco.synthesia.service.SignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static it.francesco.synthesia.api.SignatureController.COURTESY_MESSAGE;
import static it.francesco.synthesia.api.SignatureController.GIVING_SIGNATURE_MESSAGE;
import static org.junit.jupiter.api.Assertions.*;


class SignatureControllerTest {

    private SignatureController signatureController;
    public SignatureService signatureService;
    public String baseWaitingUrl;

    @BeforeEach
    void setUp() {
        this.baseWaitingUrl = "http://mysampleurl/";
        this.signatureService = Mockito.mock(SignatureService.class);

        // instantiate under test class
        this.signatureController = new SignatureController(signatureService, baseWaitingUrl);
    }

    @Test
    void signatureAPI_MessageReturnedImmediately() {

        // given
        String inputMessage = "my sample message";
        String signature = "my signature";
        Message returnedMessage = new Message();
        returnedMessage.setSignature(signature);
        returnedMessage.setMessage(inputMessage);
        returnedMessage.setStatus(MessageStatus.SIGNED);
        Mockito.when(signatureService.sign(Mockito.any(), Mockito.any())).thenReturn(returnedMessage);

        // when
        ResponseEntity<MessageResponse> response = this.signatureController.signature(inputMessage);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        MessageResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(signature, body.getSignature());
        assertEquals(GIVING_SIGNATURE_MESSAGE, body.getInfo());

    }


    @Test
    void signatureAPI_MessageNotReturnedWaitingExceeded() {

        // given
        String inputMessage = "my sample message";
        String signature = "my signature";
        String identifier = Utils.generateIdentifier(inputMessage);
        String pollingPath = "/signature/" + identifier;
        String waitingUrl = baseWaitingUrl + identifier;
        Mockito.when(signatureService.sign(Mockito.any(), Mockito.any())).thenReturn(null);

        // when
        ResponseEntity<MessageResponse> response = this.signatureController.signature(inputMessage);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        MessageResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(waitingUrl, body.getWaitingWebsite());
        assertEquals(pollingPath, body.getPollingPath());
        assertEquals(COURTESY_MESSAGE, body.getInfo());

    }

    @Test
    void getSavedAPI_IdentifierSigned() {

        // given
        String identifier = "my_identifier";
        Message message = new Message();
        String signature = "signature";
        message.setSignature(signature);
        message.setStatus(MessageStatus.SIGNED);
        Mockito.when(signatureService.getMessageById(Mockito.any())).thenReturn(Optional.of(message));

        // when
        ResponseEntity<MessageResponse> response = this.signatureController.getSaved(identifier);

        // then
        int statusCodeValue = response.getStatusCodeValue();
        MessageResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(200, statusCodeValue);
        assertEquals(signature, body.getSignature());
        assertEquals(GIVING_SIGNATURE_MESSAGE, body.getInfo());

    }


    @Test
    void getSavedAPI_IdentifierPresentButInProgress() {

        // given
        String identifier = "my_identifier";
        Message message = new Message();
        message.setStatus(MessageStatus.IN_PROGRESS);
        Mockito.when(signatureService.getMessageById(Mockito.any())).thenReturn(Optional.of(message));

        // when
        ResponseEntity<MessageResponse> response = this.signatureController.getSaved(identifier);

        // then
        int statusCodeValue = response.getStatusCodeValue();
        MessageResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(202, statusCodeValue);
        assertNull(body.getSignature());
        assertEquals(COURTESY_MESSAGE, body.getInfo());

    }


    @Test
    void getSavedAPI_IdentifierNotPresent() {

        // given
        String identifier = "my_identifier";
        Mockito.when(signatureService.getMessageById(Mockito.any())).thenReturn(Optional.empty());

        // when
        ResponseEntity<MessageResponse> response = this.signatureController.getSaved(identifier);

        // then
        int statusCodeValue = response.getStatusCodeValue();
        MessageResponse body = response.getBody();
        assertNull(body);
        assertEquals(404, statusCodeValue);

    }

}