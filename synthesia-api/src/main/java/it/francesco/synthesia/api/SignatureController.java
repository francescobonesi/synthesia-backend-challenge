package it.francesco.synthesia.api;


import it.francesco.synthesia.model.Message;
import it.francesco.synthesia.model.MessageResponse;
import it.francesco.synthesia.service.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SignatureController {

    private static final String COURTESY_MESSAGE = "We are working for you, retry later!";
    private static final String GIVING_SIGNATURE_MESSAGE = "Here is your signature!";

    public SignatureService signatureService;

    @Autowired
    public SignatureController(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @GetMapping("/crypto/sign/{identifier}")
    public ResponseEntity<MessageResponse> getSaved(@PathVariable("identifier") String identifier){
        return signatureService.getMessageById(identifier).map(value -> ResponseEntity.ok(MessageResponse.builder()
                .signature(value.getSignature())
                .info(GIVING_SIGNATURE_MESSAGE)
                .build())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/crypto/sign")
    public MessageResponse signature(@RequestParam("message") String msgStr) {

        Message returned = signatureService.sign(msgStr);

        if (returned != null) return MessageResponse.builder()
                .signature(returned.getSignature())
                .info(GIVING_SIGNATURE_MESSAGE)
                .build();
        else {
            return MessageResponse.builder()
                    .info(COURTESY_MESSAGE)
                    .build();
        }

    }

}
