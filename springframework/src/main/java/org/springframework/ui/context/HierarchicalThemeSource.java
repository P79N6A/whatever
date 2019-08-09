package org.springframework.ui.context;

import org.springframework.lang.Nullable;

public interface HierarchicalThemeSource extends ThemeSource {

    void setParentThemeSource(@Nullable ThemeSource parent);

    @Nullable
    ThemeSource getParentThemeSource();

}
