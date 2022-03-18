package com.ashelkov.owg.wallet.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *
 */
public class EncodingUtils {

    public static final int MONERO_HOP = 8;
    public static final int MONERO_RES_LONG = 11;
    public static final int MONERO_RES_SHORT = 7;

    private static final char[] BITCOIN_ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] RIPPLE_ALPHABET =
            "rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz".toCharArray();

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    /**
     * Encodes the given bytes as a base58 string (no checksum is appended) for Bitcoin.
     *
     * @param input the bytes to encode
     * @return the base58-encoded string
     */
    public static String base58Bitcoin(byte[] input) {
        return doEncodeBase58(input, BITCOIN_ALPHABET);
    }

    /**
     * Encodes the given bytes as a base58 string (no checksum is appended) for Ripple.
     *
     * @param input the bytes to encode
     * @return the base58-encoded string
     */
    public static String base58Ripple(byte[] input) {
        return doEncodeBase58(input, RIPPLE_ALPHABET);
    }

    /**
     *
     * https://monerodocs.org/cryptography/base58/
     *
     * @param input
     * @return
     */
    public static String base58Monero(byte[] input) {

        StringBuilder result = new StringBuilder();

        // TODO: Need check that input is exactly 65 bytes in length (though the current code should work for any length)
        int length = input.length;
        int hopLength = length - MONERO_HOP;
        int i;

        for (i = 0; i < hopLength; i += MONERO_HOP) {
            byte[] chunk = new byte[MONERO_HOP];
            System.arraycopy(input, i, chunk, 0, MONERO_HOP);
            String chunk58 = base58Bitcoin(chunk);

            result.append(String.valueOf(BITCOIN_ALPHABET[0]).repeat(MONERO_RES_LONG - chunk58.length()));
            result.append(chunk58);
        }

        byte[] chunk = new byte[length - i];
        System.arraycopy(input, i, chunk, 0, length - i);
        String chunk58 = base58Bitcoin(chunk);

        result.append(String.valueOf(BITCOIN_ALPHABET[0]).repeat(MONERO_RES_SHORT - chunk58.length()));
        result.append(chunk58);

        return result.toString();
    }

    /**
     *
     * NOTE: This code is copied from...
     * https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/core/Base58.java
     *
     * ================================================================================================================
     *
     * Encodes the given bytes as a base58 string (no checksum is appended).
     *
     * @param input the bytes to encode
     * @return the base58-encoded string
     */
    private static String doEncodeBase58(byte[] input, char[] alphabet) {
        if (input.length == 0) {
            return "";
        }

        char encodedZero = alphabet[0];

        // Count leading zeros.
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            ++zeros;
        }
        // Convert base-256 digits to base-58 digits (plus conversion to ASCII characters)
        input = Arrays.copyOf(input, input.length); // since we modify it in-place
        char[] encoded = new char[input.length * 2]; // upper bound
        int outputStart = encoded.length;
        for (int inputStart = zeros; inputStart < input.length; ) {
            encoded[--outputStart] = alphabet[divmod(input, inputStart, 256, 58)];
            if (input[inputStart] == 0) {
                ++inputStart; // optimization - skip leading zeros
            }
        }
        // Preserve exactly as many leading encoded zeros in output as there were leading zeros in input.
        while (outputStart < encoded.length && encoded[outputStart] == encodedZero) {
            ++outputStart;
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = encodedZero;
        }
        // Return encoded string (including encoded leading zeros).
        return new String(encoded, outputStart, encoded.length - outputStart);
    }

    /**
     * Divides a number, represented as an array of bytes each containing a single digit
     * in the specified base, by the given divisor. The given number is modified in-place
     * to contain the quotient, and the return value is the remainder.
     *
     * @param number the number to divide
     * @param firstDigit the index within the array of the first non-zero digit
     *        (this is used for optimization by skipping the leading zeros)
     * @param base the base in which the number's digits are represented (up to 256)
     * @param divisor the number to divide by (up to 256)
     * @return the remainder of the division operation
     */
    private static byte divmod(byte[] number, int firstDigit, int base, int divisor) {
        // this is just long division which accounts for the base of the input digits
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = (int) number[i] & 0xFF;
            int temp = remainder * base + digit;
            number[i] = (byte) (temp / divisor);
            remainder = temp % divisor;
        }
        return (byte) remainder;
    }

    /**
     *
     * @param input
     * @return
     */
    public static byte[] to5BitBytesSafe(byte[] input) {

        byte[] rawBytes;
        byte[] result;
        int val = input.length % 5;

        if (val == 0) {
            rawBytes = new byte[input.length];
        } else {
            rawBytes = new byte[input.length + (5 - val)];
        }
        result = new byte[(rawBytes.length / 5) * 8];

        System.arraycopy(input, 0, rawBytes, 0, input.length);

        for (int i = 0, j = 0; i < rawBytes.length; i += 5, j += 8) {
            result[j] = (byte)((rawBytes[i] >> 3) & 0x1f);
            result[j + 1] = (byte)(((rawBytes[i] & 0x07) << 2) + ((rawBytes[i + 1] >> 6) & 0x03));
            result[j + 2] = (byte)(((rawBytes[i + 1] & 0x3e) >> 1) & 0x1f);
            result[j + 3] = (byte)(((rawBytes[i + 1] & 0x01) << 4) + ((rawBytes[i + 2] >> 4) & 0x0f));
            result[j + 4] = (byte)(((rawBytes[i + 2] & 0x0f) << 1) + ((rawBytes[i + 3] >> 7) & 0x01));
            result[j + 5] = (byte)(((rawBytes[i + 3] & 0x7c) >> 2) & 0x1f);
            result[j + 6] = (byte)(((rawBytes[i + 3] & 0x03) << 3) + ((rawBytes[i + 4] >> 5) & 0x07));
            result[j + 7] = (byte)(rawBytes[i + 4] & 0x1f);
        }

        return result;
    }

    /**
     * Code taken from the top answer to this question:
     * https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
     *
     * ================================================================================================================
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        // TODO: Move to test
//        byte[] test1 = new byte[] {(byte)0xf8, (byte)0x3e, (byte)0x0f, (byte)0x83, (byte)0xe0};
//        logger.info(Arrays.toString(EncodingUtils.to5BitBytesSafe(test1)));
//        byte[] test2 = new byte[] {(byte)0x07, (byte)0xc1, (byte)0xf0, (byte)0x7c, (byte)0x1f};
//        logger.info(Arrays.toString(EncodingUtils.to5BitBytesSafe(test2)));

        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static byte[] intToFourBytes(int i, boolean isLittleEndian) {
        byte[] result = new byte[4];

        if (isLittleEndian) {
            for (int j = 0; j < 4; j++) {
                result[j] = (byte)(i >> (j * 8));
            }
        } else {
            for (int j = 0; j < 4; j++) {
                result[3 - j] = (byte)(i >> (j * 8));
            }
        }

        return result;
    }

    private EncodingUtils() {}
}
