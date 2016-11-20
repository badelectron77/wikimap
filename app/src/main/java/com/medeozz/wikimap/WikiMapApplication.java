package com.medeozz.wikimap;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;
import org.acra.sender.HttpSender;

// Dokumentation https://github.com/ACRA/acra/wiki/AdvancedUsage

/*
        formUriBasicAuthLogin = "wikimap@medeozz.com",
        formUriBasicAuthPassword = "y0uRpa$$w0rd001",
 */

@ReportsCrashes(
        formUri = "http://medeozz.com/wikimap_crashes.php",
        httpMethod = org.acra.sender.HttpSender.Method.POST,
        mode = ReportingInteractionMode.SILENT,
        reportType = HttpSender.Type.FORM)
public class WikiMapApplication extends Application {

    private MyStateManager myStateManager = new MyStateManager();

    public MyStateManager getStateManager() {
        return myStateManager;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);

    }
}
