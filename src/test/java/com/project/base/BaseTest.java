package com.project.base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.net.URI;
import java.time.Duration;

public class BaseTest {
    protected AndroidDriver driver;
    protected final String appPackage = "rock.identifier.diamond.gem.stone.mineral.finder.scanner";

    // REPORTING VARIABLES
    protected static ExtentReports extent;
    protected static ExtentTest testLog;
    protected int totalBugCount = 0;

    @BeforeSuite
    public void setupReport() {
        ExtentSparkReporter spark = new ExtentSparkReporter(System.getProperty("user.dir") + "/target/Automation_Report.html");
        spark.config().setDocumentTitle("House Of Apps - Test Report");
        spark.config().setReportName("Stone Identifier Automation Suite");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Device", "Xiaomi POCO F1");
        extent.setSystemInfo("Platform", "Android 10");
    }

    @BeforeMethod
    public void setupDriver() throws Exception {
        totalBugCount = 0; // Reset bug counter
        testLog = extent.createTest("Master Onboarding & FTUE Flow"); // Start logging

        String appTarget = System.getProperty("targetApp", "Stone");

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("83fc08ef");
        options.setNoReset(true);
        options.setCapability("appium:autoLaunch", false);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);
        options.setCapability("appium:ignoreUnimportantViews", true);
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:waitForIdleTimeout", 100);

        if (appTarget.equalsIgnoreCase("Stone")) {
            options.setAppPackage(appPackage);
            options.setAppActivity(appPackage + ".MainActivity");
        }

        driver = new AndroidDriver(new URI("http://127.0.0.1:4723").toURL(), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    // ==========================================
    // UNIFIED REPORTING METHODS
    // ==========================================

    /** Logs a standard step to the console AND the HTML report */
    public void logStep(String message) {
        System.out.println(message);
        if(testLog != null) testLog.info(message);
    }

    /** Takes a screenshot, increments bug count, and embeds image in HTML report */
    public void reportBug(String bugMessage, String screenshotName) {
        totalBugCount++;
        System.out.println("❌ BUG FOUND: " + bugMessage);
        try {
            File scrFile = driver.getScreenshotAs(OutputType.FILE);
            String fileName = screenshotName + "_" + System.currentTimeMillis() + ".png";
            String filePath = System.getProperty("user.dir") + "/target/bug_screenshots/" + fileName;
            FileUtils.copyFile(scrFile, new File(filePath));

            if(testLog != null) {
                testLog.fail("BUG: " + bugMessage, MediaEntityBuilder.createScreenCaptureFromPath(filePath).build());
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to take screenshot: " + e.getMessage());
            if(testLog != null) testLog.fail("BUG: " + bugMessage + " (Screenshot Failed)");
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    public void setNetworkState(boolean internetEnabled) {
        try {
            if (internetEnabled) {
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("wifi", "enable")));
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("data", "enable")));
                logStep("[SYSTEM] Internet turned ON (via ADB shell).");
            } else {
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("wifi", "disable")));
                driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("data", "disable")));
                logStep("[SYSTEM] Internet turned OFF (via ADB shell).");
            }
            Thread.sleep(3000);
        } catch (Exception e) {
            logStep("[ERROR] Failed to toggle network. Did you start Appium with --relaxed-security?");
        }
    }

    @SuppressWarnings("BusyWait")
    public void closeInterstitialAds() {
        logStep("[ACTION] AdBuster Engaged: Hunting for 'Skip' or 'Close' buttons...");
        long endTime = System.currentTimeMillis() + 60000;

        while (System.currentTimeMillis() < endTime) {
            try {
                if (!driver.findElements(AppiumBy.xpath("//*[@text='Grant Permission'] | //*[@text='Continue'] | //*[@text='Identify'] | //*[@text='Watch Ad for 1 Use'] | //*[@text='Crop Stone'] | //*[@text='DONE']")).isEmpty()) {
                    logStep("[ACTION] Target UI detected. Ad successfully bypassed.");
                    return;
                }

                var adCounters = driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Ad ')] | //*[contains(@text, 'Reward')]"));
                if (!adCounters.isEmpty()) {
                    System.out.println("   -> Ad Status: " + adCounters.getFirst().getText());
                }

                String closeXPath = "//*[@content-desc='Close' or @content-desc='close' or @text='Skip' or @text='Close' or @text='X' or contains(@resource-id, 'close') or contains(@resource-id, 'dismiss')] | //*[contains(@text, 'Reward')]/..//android.widget.Image | //*[contains(@text, 'Reward')]/following-sibling::*";
                var closeBtns = driver.findElements(AppiumBy.xpath(closeXPath));

                if (!closeBtns.isEmpty()) {
                    closeBtns.getFirst().click();
                    logStep("[ACTION] Tapped an Ad Close/Skip button.");
                    Thread.sleep(2000);
                } else {
                    Thread.sleep(2000);
                }
            } catch (Exception ignored) {}
        }
        logStep("[WARNING] AdBuster timed out after 60 seconds.");
    }
    @SuppressWarnings("unused")
    public void clearAppData() {
        try {
            logStep("[SYSTEM] Terminating app and clearing all app data...");
            driver.terminateApp(appPackage);
            driver.executeScript("mobile: clearApp", java.util.Map.of("appId", appPackage));
            logStep("[SYSTEM] App data cleared successfully.");
        } catch (Exception e) {
            logStep("[ERROR] Failed to clear app data: " + e.getMessage());
        }
    }

    @AfterMethod
    public void tearDown() {
        // clearAppData(); // Disabled to view Crop UI at the end
        if (driver != null) {
            driver.quit();
        }
    }

    @AfterSuite
    public void tearDownReport() {
        if (extent != null) {
            extent.flush();
        }
    }
}