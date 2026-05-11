package com.project.utils;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import java.io.File;

public class AppiumServerManager {
    private static AppiumDriverLocalService service;

    public static void startServer() {
        if (service == null || !service.isRunning()) {
            AppiumServiceBuilder builder = new AppiumServiceBuilder();
            builder.withIPAddress("127.0.0.1");
            builder.usingPort(4723);
            builder.withArgument(GeneralServerFlag.RELAXED_SECURITY);

            // 🔥 SILENCER 1: Tell Appium to stop generating info/debug chatter
            builder.withArgument(GeneralServerFlag.LOG_LEVEL, "error");

            // Route whatever is left to the background file
            builder.withLogFile(new File("target/appium_server.log"));

            service = AppiumDriverLocalService.buildService(builder);

            // 🔥 SILENCER 2: Sever the connection between Appium's output and your Java console
            service.clearOutPutStreams();

            service.start();

            System.out.println("\n[SYSTEM] 🚀 Appium Server started programmatically at " + service.getUrl() + " [RELAXED SECURITY: ENABLED]");
            System.out.println("[SYSTEM] 🤫 Appium logs muted in terminal. Check target/appium_server.log for background data.");
        }
    }

    public static java.net.URL getServiceUrl() {
        return service.getUrl();
    }

    public static void stopServer() {
        if (service != null && service.isRunning()) {
            service.stop();
            System.out.println("[SYSTEM] 🛑 Appium Server stopped automatically.");
        }
    }
}