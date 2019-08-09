package org.springframework.format.number.money;

import org.springframework.format.Formatter;
import org.springframework.lang.Nullable;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.util.Locale;

public class MonetaryAmountFormatter implements Formatter<MonetaryAmount> {

    @Nullable
    private String formatName;

    public MonetaryAmountFormatter() {
    }

    public MonetaryAmountFormatter(String formatName) {
        this.formatName = formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    @Override
    public String print(MonetaryAmount object, Locale locale) {
        return getMonetaryAmountFormat(locale).format(object);
    }

    @Override
    public MonetaryAmount parse(String text, Locale locale) {
        return getMonetaryAmountFormat(locale).parse(text);
    }

    protected MonetaryAmountFormat getMonetaryAmountFormat(Locale locale) {
        if (this.formatName != null) {
            return MonetaryFormats.getAmountFormat(this.formatName);
        } else {
            return MonetaryFormats.getAmountFormat(locale);
        }
    }

}
