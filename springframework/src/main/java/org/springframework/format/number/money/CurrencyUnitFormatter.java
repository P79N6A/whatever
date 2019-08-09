package org.springframework.format.number.money;

import org.springframework.format.Formatter;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

public class CurrencyUnitFormatter implements Formatter<CurrencyUnit> {

    @Override
    public String print(CurrencyUnit object, Locale locale) {
        return object.getCurrencyCode();
    }

    @Override
    public CurrencyUnit parse(String text, Locale locale) {
        return Monetary.getCurrency(text);
    }

}
