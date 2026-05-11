package com.project.utils;

import io.appium.java_client.android.AndroidDriver;
import java.util.Arrays;
import java.util.Map;

public class SystemUtils {

    public static void setNetworkState(AndroidDriver driver, boolean internetEnabled) {
        try {
            if (internetEnabled) {
                driver.executeScript("mobile: shell", Map.of("command", "svc", "args", Arrays.asList("wifi", "enable")));
                driver.executeScript("mobile: shell", Map.of("command", "svc", "args", Arrays.asList("data", "enable")));
                ReportManager.logStep("[SYSTEM] Internet turned ON.");
            } else {
                driver.executeScript("mobile: shell", Map.of("command", "svc", "args", Arrays.asList("wifi", "disable")));
                driver.executeScript("mobile: shell", Map.of("command", "svc", "args", Arrays.asList("data", "disable")));
                ReportManager.logStep("[SYSTEM] Internet turned OFF.");
            }
            Thread.sleep(3000);
        } catch (Exception e) {
            ReportManager.logStep("[ERROR] Failed to toggle network.");
        }
    }

    public static void clearAppData(AndroidDriver driver, String appPackage) {
        try {
            ReportManager.logStep("[SYSTEM] Terminating app and clearing all app data...");
            driver.terminateApp(appPackage);
            driver.executeScript("mobile: clearApp", Map.of("appId", appPackage));
            ReportManager.logStep("[SYSTEM] App data cleared successfully.");
        } catch (Exception e) {
            ReportManager.logStep("[ERROR] Failed to clear app data: " + e.getMessage());
        }
    }
}