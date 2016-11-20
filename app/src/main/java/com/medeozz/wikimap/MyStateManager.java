package com.medeozz.wikimap;

import java.util.Locale;

public class MyStateManager {

    private boolean mapScrolling;
    private static String systemLanguage;

    MyStateManager() {

        mapScrolling = false;

        systemLanguage = Locale.getDefault().getLanguage();
        if(systemLanguage == null || systemLanguage.trim().length() == 0) {
            systemLanguage = "en";
        }
    }

    public boolean getMapScrolling() {
        return mapScrolling;
    }

    public void setMapScrolling(boolean value) {
        mapScrolling = value;
    }

    public String getSystemLanguage() {
        return systemLanguage;
    }
}
