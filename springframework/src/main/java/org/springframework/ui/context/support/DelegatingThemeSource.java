package org.springframework.ui.context.support;

import org.springframework.lang.Nullable;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

public class DelegatingThemeSource implements HierarchicalThemeSource {

    @Nullable
    private ThemeSource parentThemeSource;

    @Override
    public void setParentThemeSource(@Nullable ThemeSource parentThemeSource) {
        this.parentThemeSource = parentThemeSource;
    }

    @Override
    @Nullable
    public ThemeSource getParentThemeSource() {
        return this.parentThemeSource;
    }

    @Override
    @Nullable
    public Theme getTheme(String themeName) {
        if (this.parentThemeSource != null) {
            return this.parentThemeSource.getTheme(themeName);
        } else {
            return null;
        }
    }

}
