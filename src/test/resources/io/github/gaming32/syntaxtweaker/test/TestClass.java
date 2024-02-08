package io.github.gaming32.syntaxtweaker.test;

public class TestClass {
    public static int shouldBeHex;

    public static void main(String[] args) {
        int hex1 = 15;
        final int octal2 = 9;
        hex1 = 127;
        shouldBeHex = hex1;
        shouldBeOctal(octal2);
    }

    public static void shouldBeOctal(int arg) {
    }
}
