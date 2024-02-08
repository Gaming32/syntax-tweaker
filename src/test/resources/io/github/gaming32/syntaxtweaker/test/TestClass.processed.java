package io.github.gaming32.syntaxtweaker.test;

public class TestClass {
    public static int shouldBeHex;

    public static void main(String[] args) {
        int hex1 = 0xf;
        final int octal2 = 011;
        hex1 = 0x7f;
        shouldBeHex = hex1;
        shouldBeOctal(octal2);
    }

    public static void shouldBeOctal(int arg) {
    }
}
