package org.springframework.web.util;

import org.springframework.util.Assert;

public abstract class HtmlUtils {

    private static final HtmlCharacterEntityReferences characterEntityReferences = new HtmlCharacterEntityReferences();

    public static String htmlEscape(String input) {
        return htmlEscape(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
    }

    public static String htmlEscape(String input, String encoding) {
        Assert.notNull(input, "Input is required");
        Assert.notNull(encoding, "Encoding is required");
        StringBuilder escaped = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            String reference = characterEntityReferences.convertToReference(character, encoding);
            if (reference != null) {
                escaped.append(reference);
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    public static String htmlEscapeDecimal(String input) {
        return htmlEscapeDecimal(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
    }

    public static String htmlEscapeDecimal(String input, String encoding) {
        Assert.notNull(input, "Input is required");
        Assert.notNull(encoding, "Encoding is required");
        StringBuilder escaped = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (characterEntityReferences.isMappedToReference(character, encoding)) {
                escaped.append(HtmlCharacterEntityReferences.DECIMAL_REFERENCE_START);
                escaped.append((int) character);
                escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    public static String htmlEscapeHex(String input) {
        return htmlEscapeHex(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
    }

    public static String htmlEscapeHex(String input, String encoding) {
        Assert.notNull(input, "Input is required");
        Assert.notNull(encoding, "Encoding is required");
        StringBuilder escaped = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (characterEntityReferences.isMappedToReference(character, encoding)) {
                escaped.append(HtmlCharacterEntityReferences.HEX_REFERENCE_START);
                escaped.append(Integer.toString(character, 16));
                escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    public static String htmlUnescape(String input) {
        return new HtmlCharacterEntityDecoder(characterEntityReferences, input).decode();
    }

}
