package it.francesco.synthesia.job.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "synthesia", url="${synthesia.url}")
public interface SynthesiaClient {

    @GetMapping("/crypto/sign")
    String getSignature(@RequestParam("message") String message);

    @GetMapping("/crypto/verify")
    String verifySignature(@RequestParam("message") String message, @RequestParam("signature") String signature);

}
