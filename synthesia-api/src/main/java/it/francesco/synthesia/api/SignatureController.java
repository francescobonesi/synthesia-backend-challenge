package it.francesco.synthesia.api;


import it.francesco.synthesia.model.Message;
import it.francesco.synthesia.model.MessageResponse;
import it.francesco.synthesia.model.MessageStatus;
import it.francesco.synthesia.service.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static it.francesco.synthesia.Utils.generateIdentifier;

@RestController
@Slf4j
public class SignatureController {

    static final String COURTESY_MESSAGE = "We are working for you, don't worry!";
    static final String GIVING_SIGNATURE_MESSAGE = "Here is your signature!";

    public SignatureService signatureService;
    public String baseWaitingUrl;

    @Autowired
    public SignatureController(SignatureService signatureService,
                               @Value("${synthesia.baseWaitingUrl}") String baseWaitingUrl) {
        this.baseWaitingUrl = baseWaitingUrl;
        this.signatureService = signatureService;
    }

    @GetMapping("/signature/{identifier}")
    public ResponseEntity<MessageResponse> getSaved(@PathVariable("identifier") String identifier) {

        Optional<Message> optionalMessage = signatureService.getMessageById(identifier);

        if (optionalMessage.isEmpty()) return ResponseEntity.notFound().build();

        Message message = optionalMessage.get();

        if (message.getStatus().equals(MessageStatus.SIGNED)) {
            return ResponseEntity.ok(MessageResponse.builder()
                    .signature(message.getSignature())
                    .info(GIVING_SIGNATURE_MESSAGE)
                    .build());
        } else {
            return ResponseEntity.accepted().body(MessageResponse.builder()
                    .info(COURTESY_MESSAGE)
                    .build());
        }


    }

    private MessageResponse buildMessageFromResponse(Message returned, String identifier) {
        if (returned != null) return MessageResponse.builder()
                .signature(returned.getSignature())
                .info(GIVING_SIGNATURE_MESSAGE)
                .build();
        else {
            return MessageResponse.builder()
                    .info(COURTESY_MESSAGE)
                    .waitingWebsite(baseWaitingUrl + identifier)
                    .pollingPath("/signature/" + identifier)
                    .build();
        }
    }

    @GetMapping("/crypto/sign")
    public ResponseEntity<MessageResponse> signature(@RequestParam("message") String msgStr) {

        String identifier = generateIdentifier(msgStr);
        Message returned = signatureService.sign(msgStr, identifier);
        return ResponseEntity.ok(buildMessageFromResponse(returned, identifier));

    }

}
