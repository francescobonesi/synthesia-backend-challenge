package it.francesco.synthesia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void generateIdentifier() {

        // when
        String output = Utils.generateIdentifier("sample test!");

        // then
        assertEquals("4bcaa878846bb9bd44880fa692cbe43445acc42fd2a865a11029c62ed85906df", output);

    }

    @Test
    void generateIdentifierEvenIfEmptyString() {

        // when
        String output = Utils.generateIdentifier("");

        // then
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", output);

    }
}