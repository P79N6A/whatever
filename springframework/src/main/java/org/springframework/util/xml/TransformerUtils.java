package org.springframework.util.xml;

import org.springframework.util.Assert;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

public abstract class TransformerUtils {

    public static final int DEFAULT_INDENT_AMOUNT = 2;

    public static void enableIndenting(Transformer transformer) {
        enableIndenting(transformer, DEFAULT_INDENT_AMOUNT);
    }

    public static void enableIndenting(Transformer transformer, int indentAmount) {
        Assert.notNull(transformer, "Transformer must not be null");
        if (indentAmount < 0) {
            throw new IllegalArgumentException("Invalid indent amount (must not be less than zero): " + indentAmount);
        }
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        try {
            // Xalan-specific, but this is the most common XSLT engine in any case
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", String.valueOf(indentAmount));
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void disableIndenting(Transformer transformer) {
        Assert.notNull(transformer, "Transformer must not be null");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
    }

}
