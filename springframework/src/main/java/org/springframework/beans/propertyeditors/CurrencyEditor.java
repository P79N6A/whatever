package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Currency;

public class CurrencyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(Currency.getInstance(text));
    }

    @Override
    public String getAsText() {
        Currency value = (Currency) getValue();
        return (value != null ? value.getCurrencyCode() : "");
    }

}
