package com.project.base;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.connection.ConnectionStateBuilder;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.io.File;
import java.net.URI;
import java.time.Duration;

public class BaseTest {
    protected AndroidDriver driver;
    protected final String appPackage = "rock.identifier.diamond.gem.stone.mineral.finder.scanner";

    @BeforeMethod
    public void setupDriver() throws Exception {
        String appTarget = System.getProperty("targetApp", "Stone");

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("83fc08ef"); // Your POCO F1
        options.setNoReset(true);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        if (appTarget.equalsIgnoreCase("Stone")) {
            options.setAppPackage(appPackage);
            options.setAppActivity(appPackage + ".MainActivity");
        }

        driver = new AndroidDriver(new URI("http://127.0.0.1:4723").toURL(), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    // ==========================================
    // UTILITY METHODS FOR THE TEST SCRIPT
    // ==========================================

    /** Toggles the device Wi-Fi and Data */
    /** Toggles the device Wi-Fi and Data using raw ADB shell commands */
    public void setNetworkState(boolean internetEnabled) {
        try {
            if (internetEnabled) {
                // Enable WiFi and Data
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("wifi", "enable")));
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("data", "enable")));
                System.out.println("[SYSTEM] Internet turned ON (via ADB shell).");
            } else {
                // Disable WiFi and Data
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("wifi", "disable")));
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("data", "disable")));
                System.out.println("[SYSTEM] Internet turned OFF (via ADB shell).");
            }
            Thread.sleep(3000); // Give the OS 3 seconds to drop/reconnect the signal
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to toggle network: " + e.getMessage());
            System.out.println("-> DID YOU FORGET TO START APPIUM WITH --relaxed-security?");
        }
    }

    /** Takes a screenshot and saves it to the target folder */
    public void takeBugScreenshot(String bugName) {
        try {
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filePath = System.getProperty("user.dir") + "/target/bug_screenshots/" + bugName + "_" + System.currentTimeMillis() + ".png";
            FileUtils.copyFile(scrFile, new File(filePath));
            System.out.println("[BUG] Screenshot saved at: " + filePath);
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to take screenshot: " + e.getMessage());
        }
    }

    /** Wipes the app data from the phone like a fresh install */
    /** Wipes the app data from the phone like a fresh install */
    // Keep your existing setupDriver(), setNetworkState(), and takeBugScreenshot() methods above this...

    /** Wipes the app data from the phone like a fresh install */
    public void clearAppData() {
        try {
            System.out.println("[SYSTEM] Terminating app and clearing all app data...");
            driver.terminateApp(appPackage);
            driver.executeScript("mobile: clearApp", java.util.Map.of("appId", appPackage));
            System.out.println("[SYSTEM] App data cleared successfully.");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to clear app data: " + e.getMessage());
        }
    }

    @AfterMethod
    public void tearDown() {
        // ALWAYS clear data before quitting the driver, even if the test fails!
        clearAppData();

        if (driver != null) {
            driver.quit();
        }
    }
}