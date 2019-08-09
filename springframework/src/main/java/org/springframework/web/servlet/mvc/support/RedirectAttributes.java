package org.springframework.web.servlet.mvc.support;

import org.springframework.lang.Nullable;
import org.springframework.ui.Model;

import java.util.Collection;
import java.util.Map;

public interface RedirectAttributes extends Model {

    @Override
    RedirectAttributes addAttribute(String attributeName, @Nullable Object attributeValue);

    @Override
    RedirectAttributes addAttribute(Object attributeValue);

    @Override
    RedirectAttributes addAllAttributes(Collection<?> attributeValues);

    @Override
    RedirectAttributes mergeAttributes(Map<String, ?> attributes);

    RedirectAttributes addFlashAttribute(String attributeName, @Nullable Object attributeValue);

    RedirectAttributes addFlashAttribute(Object attributeValue);

    Map<String, ?> getFlashAttributes();

}
