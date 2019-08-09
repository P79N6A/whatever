package org.springframework.ui.context;

import org.springframework.lang.Nullable;

public interface ThemeSource {

    @Nullable
    Theme getTheme(String themeName);

}
