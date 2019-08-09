package org.springframework.transaction.interceptor;

import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.Enumeration;
import java.util.Properties;

public class TransactionAttributeSourceEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
        if (StringUtils.hasLength(text)) {
            // Use properties editor to tokenize the hold string.
            PropertiesEditor propertiesEditor = new PropertiesEditor();
            propertiesEditor.setAsText(text);
            Properties props = (Properties) propertiesEditor.getValue();
            // Now we have properties, process each one individually.
            TransactionAttributeEditor tae = new TransactionAttributeEditor();
            Enumeration<?> propNames = props.propertyNames();
            while (propNames.hasMoreElements()) {
                String name = (String) propNames.nextElement();
                String value = props.getProperty(name);
                // Convert value to a transaction attribute.
                tae.setAsText(value);
                TransactionAttribute attr = (TransactionAttribute) tae.getValue();
                // Register name and attribute.
                source.addTransactionalMethod(name, attr);
            }
        }
        setValue(source);
    }

}
