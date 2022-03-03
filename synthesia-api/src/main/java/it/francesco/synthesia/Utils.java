package it.francesco.synthesia;

import org.apache.commons.codec.digest.DigestUtils;

public class Utils {

    private Utils() {
        // cannot instantiate Utils, only static methods
    }

    public static String generateIdentifier(String inputText) {
        return DigestUtils.sha256Hex(inputText);
    }
}
