package org.springframework.web.servlet.tags.form;

@SuppressWarnings("serial")
public class RadioButtonsTag extends AbstractMultiCheckedElementTag {

    @Override
    protected String getInputType() {
        return "radio";
    }

}
