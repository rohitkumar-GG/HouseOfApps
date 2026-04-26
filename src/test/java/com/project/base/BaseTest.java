package com.project.base;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
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
        options.setDeviceName("83fc08ef");
        options.setNoReset(true);
        options.setCapability("appium:autoLaunch", false);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        // =========================================
        // THE SPEED BOOSTERS
        // =========================================
        // 1. Forces Appium to ignore layout containers and only look at interactable elements
        options.setCapability("appium:ignoreUnimportantViews", true);
        // 2. Disables window animations so Appium doesn't hang waiting for the screen to settle
        options.setCapability("appium:disableWindowAnimation", true);
        // 3. Reduces the internal idle timeout from 10 seconds to near-zero
        options.setCapability("appium:waitForIdleTimeout", 100);

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

    // ==========================================
    // NEW: THE AD BUSTER UTILITY
    // ==========================================
    /** * Dynamically waits for and closes interstitial video ads.
     * It scans the screen every 2 seconds for common "Close" or "Skip" buttons.
     */
    // ==========================================
    // UPGRADED AD BUSTER (GHOST BUTTON DEFEATER)
    // ==========================================
    public void closeInterstitialAds() {
        System.out.println("[ACTION] AdBuster Engaged: Hunting for 'Skip' or 'Close' buttons...");
        long endTime = System.currentTimeMillis() + 60000; // Max wait of 60 seconds

        while (System.currentTimeMillis() < endTime) {
            try {
                // 1. Target Check: Added 'Crop Stone' and 'DONE' so it knows when it has reached the post-camera UI!
                if (!driver.findElements(AppiumBy.xpath("//*[@text='Grant Permission'] | //*[@text='Continue'] | //*[@text='Identify'] | //*[@text='Watch Ad for 1 Use'] | //*[@text='Crop Stone'] | //*[@text='DONE']")).isEmpty()) {
                    System.out.println("[ACTION] Target UI detected. Ad successfully bypassed.");
                    return;
                }

                // 2. Status Check: Look for counters OR the "Reward granted" text
                var adCounters = driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Ad ')] | //*[contains(@text, 'Reward')]"));
                if (!adCounters.isEmpty()) {
                    System.out.println("   -> Ad Status: " + adCounters.get(0).getText());
                }

                // 3. The Strike: Standard Close Buttons OR the Sneaky Relative XPath for the Ghost Button
                String closeXPath = "//*[@content-desc='Close' or @content-desc='close' or @text='Skip' or @text='Close' or @text='X' or contains(@resource-id, 'close') or contains(@resource-id, 'dismiss')] | //*[contains(@text, 'Reward')]/..//android.widget.Image | //*[contains(@text, 'Reward')]/following-sibling::*";
                var closeBtns = driver.findElements(AppiumBy.xpath(closeXPath));

                if (!closeBtns.isEmpty()) {
                    closeBtns.get(0).click();
                    System.out.println("[ACTION] Tapped an Ad Close/Skip button.");
                    Thread.sleep(2000); // Give UI time to transition
                } else {
                    Thread.sleep(2000); // Wait 2 seconds and scan again
                }
            } catch (Exception ignored) {}
        }
        System.out.println("[WARNING] AdBuster timed out after 60 seconds.");
    }

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

    // ==========================================
    // UPDATED TEARDOWN (DISABLED APP WIPE)
    // ==========================================
    @AfterMethod
    public void tearDown() {
        // DISABLED FOR STEP 19: We want the app to stay open so you can view the Crop UI!
        // clearAppData();

        if (driver != null) {
            driver.quit();
        }
    }
}