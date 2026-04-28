package com.project.base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
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
    protected String appPackage;

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
        totalBugCount = 0;
        testLog = extent.createTest("Automation Execution Flow");

        String appTarget = System.getProperty("targetApp", "Stone");

        // DYNAMIC PACKAGE ASSIGNMENT
        if (appTarget.equalsIgnoreCase("Coin")) {
            appPackage = "app.coinidentifier.checker.scanner";
        } else {
            appPackage = "rock.identifier.diamond.gem.stone.mineral.finder.scanner";
        }

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("83fc08ef"); // Update if your device ID changes
        options.setNoReset(true);
        options.setCapability("appium:autoLaunch", false);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);
        options.setCapability("appium:ignoreUnimportantViews", true);
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:waitForIdleTimeout", 100);

        // 🔥 THE FIX: Prevent Appium from killing the session during manual interventions!
        options.setCapability("appium:newCommandTimeout", 3600);

        options.setAppPackage(appPackage);
        options.setAppActivity(appPackage + ".MainActivity");

        driver = new AndroidDriver(new URI("http://127.0.0.1:4723").toURL(), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    public void logStep(String message) {
        System.out.println(message);
        if(testLog != null) testLog.info(message);
    }

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
            logStep("[ERROR] Failed to toggle network.");
        }
    }

    // ==========================================
    // THE SAFE-CLICK PLAY STORE RECOVERY
    // ==========================================
    public void safeClick(By locator, String elementName) {
        logStep("[ACTION] Safe-clicking: " + elementName);
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            org.openqa.selenium.WebElement element = driver.findElement(locator);
            element.click();
            Thread.sleep(1500); // Allow OS transition if Play Store was hit

            String currentPkg = driver.getCurrentPackage();
            if (currentPkg != null && !currentPkg.equals(appPackage)) {
                logStep("⚠️ Ad Intercepted! Opened Play Store/Browser (" + currentPkg + "). Recovering...");
                driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
                Thread.sleep(2000);

                if (!driver.getCurrentPackage().equals(appPackage)) {
                    driver.activateApp(appPackage);
                    Thread.sleep(2000);
                }

                logStep("[ACTION] Re-attempting via Bottom-Edge Coordinate Strike...");
                // THE FIX: Re-find the element because the old one is stale after leaving the app!
                element = driver.findElement(locator);
                org.openqa.selenium.Point loc = element.getLocation();
                org.openqa.selenium.Dimension size = element.getSize();
                // Tap 5 pixels above the absolute bottom edge of the button to slip under the ad's hitbox
                int safeY = loc.getY() + size.getHeight() - 5;
                int safeX = loc.getX() + (size.getWidth() / 2);
                performTap(safeX, safeY);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logStep("[WARNING] Safe-click element not found/stale: " + elementName);
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }

    // ==========================================
    // NEW: AUTO-POPUP DISMISSER
    // ==========================================
    public void dismissPopups() {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            var popups = driver.findElements(AppiumBy.xpath("//*[@text='Not Now' or @text='Cancel' or @text='No Thanks' or @text='Later' or @text='Decline' or contains(@resource-id, 'close_btn') or contains(@resource-id, 'dismiss')]"));
            if (!popups.isEmpty()) {
                popups.getFirst().click();
                logStep("[ACTION] Automatically dismissed an in-app popup.");
                Thread.sleep(1000);
            }
        } catch (Exception ignored) {}
        finally { driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); }
    }

    // ==========================================
    // UPGRADED AD BUSTERS
    // ==========================================

    @SuppressWarnings("BusyWait")
    public void closeInterstitialAds() {
        logStep("[ACTION] AdBuster Engaged: Quick-scanning for Ads or Target UI...");
        long endTime = System.currentTimeMillis() + 120000;

        // 🔥 THE SPEED FIX: Drop the wait time to 500ms so Appium doesn't freeze looking for missing ads!
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

        try {
            while (System.currentTimeMillis() < endTime) {
                // 1. FAST-EXIT: Check if we are already safe!
                boolean isSafeUiVisible = !driver.findElements(AppiumBy.xpath(
                        "//*[contains(@content-desc, 'Capture')] | " +
                                "//*[contains(@text, 'STEP 1') or contains(@text, 'Step 1')] | " +
                                "//*[contains(@text, 'STEP 2') or contains(@text, 'Step 2')] | " +
                                "//*[@text='Identify Your Rock'] | " +
                                "//*[@text='Grant Permission'] | " +
                                "//*[@text='Continue'] | " +
                                "//*[@text='Identify a Rock'] | " +
                                "//*[@text='Watch Ad for 1 Use'] | " +
                                "//*[@text='Crop Stone'] | " +
                                "//*[@text='DONE'] | " +
                                "//*[contains(@content-desc, 'Save')] | " +
                                "//*[@content-desc='Back'] | " +
                                "//*[@text='My Collection'] | " +
                                "//*[@text='Discover Stones'] | " +
                                "//*[@text='Popular Rocks'] | " +
                                "//*[@content-desc='Scan'] | " +
                                "//*[@text='My Coins']"
                )).isEmpty();

                boolean hasRewardText = !driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Reward')]")).isEmpty();

                // If Safe UI is there AND there is no active "Reward" timer -> Exit instantly!
                if (isSafeUiVisible && !hasRewardText) {
                    logStep("[ACTION] Target UI detected instantly. No ad blocking. Proceeding...");
                    return;
                }

                var adCounters = driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Ad ')] | //*[contains(@text, 'Reward')]"));
                if (!adCounters.isEmpty()) System.out.println("   -> Ad Status: " + adCounters.getFirst().getText());

                // 2. Identify Full-Screen WebViews (Embedded ads vs Interstitials)
                boolean isFullScreenWebView = false;
                var webViews = driver.findElements(AppiumBy.className("android.webkit.WebView"));
                if (!webViews.isEmpty()) {
                    try {
                        int wvHeight = webViews.getFirst().getSize().getHeight();
                        int screenHeight = driver.manage().window().getSize().getHeight();
                        if (wvHeight > (screenHeight * 0.7)) {
                            isFullScreenWebView = true;
                        }
                    } catch (Exception ignored) {}
                }

                // 3. Look for explicit close buttons
                String closeXPath = "//*[@content-desc='Close' or @content-desc='close' or @text='Skip' or @text='Close' or @text='X' or contains(@resource-id, 'close') or contains(@resource-id, 'dismiss') or (contains(@text, 'Continue') and contains(@text, 'app')) or (contains(@text, 'Continue') and contains(@text, 'App'))] | //*[contains(@text, 'Reward')]/..//android.widget.Image | //*[contains(@text, 'Reward')]/following-sibling::*";
                var closeBtns = driver.findElements(AppiumBy.xpath(closeXPath));

                if (!closeBtns.isEmpty()) {
                    closeBtns.getFirst().click();
                    logStep("[ACTION] Tapped an Ad Close/Skip button.");
                    Thread.sleep(1500);

                    // EARLY EXIT POPUP INTERCEPTOR
                    var confirmPopups = driver.findElements(AppiumBy.xpath("//*[@text='Close video' or @text='CLOSE VIDEO' or @text='Close Video' or @text='Close ad' or @text='CLOSE AD' or @text='Close Ad' or @text='CLOSE' or @text='Close' or @text='QUIT' or @text='Quit']"));
                    if (!confirmPopups.isEmpty()) {
                        logStep("[ACTION] Intercepted 'Close Video?' Warning Popup. Forcing early exit...");
                        confirmPopups.getFirst().click();
                        Thread.sleep(1500);
                    }
                } else if (isFullScreenWebView) {
                    logStep("[ACTION] Full-Screen WebView Ad active. Firing Sweeping Coordinate Strikes...");
                    org.openqa.selenium.Dimension size = driver.manage().window().getSize();

                    performTap(size.getWidth() - 50, 160); // Strike 1: Standard 'X'
                    Thread.sleep(1000);

                    var confirmPopups = driver.findElements(AppiumBy.xpath("//*[@text='Close video' or @text='CLOSE VIDEO' or @text='Close Video' or @text='Close ad' or @text='CLOSE AD' or @text='Close Ad' or @text='CLOSE' or @text='Close' or @text='QUIT' or @text='Quit']"));
                    if (!confirmPopups.isEmpty()) {
                        logStep("[ACTION] Intercepted 'Close Video?' Warning Popup after blind strike. Forcing exit...");
                        confirmPopups.getFirst().click();
                        Thread.sleep(1000);
                    }

                    if (!driver.findElements(AppiumBy.className("android.webkit.WebView")).isEmpty()) {
                        performTap(size.getWidth() - 250, 140); // Strike 2: Continue to App pill
                        Thread.sleep(1000);
                    }
                } else {
                    // Nothing found, micro-sleep before checking again so we don't fry the CPU
                    Thread.sleep(500);
                }
            }
            logStep("[WARNING] AdBuster timed out after 120s. Proceeding to fallback...");
        } catch (Exception e) {
            logStep("[WARNING] AdBuster interrupted: " + e.getMessage());
        }finally {
            // 🔥 CRITICAL: Restore the standard 10-second wait so the rest of your test suite doesn't fail!
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }

    public void collapseHalfScreenAd() {
        logStep("[ACTION] Checking for native half-screen ad banner...");
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            var explicitClose = driver.findElements(AppiumBy.xpath("//*[@content-desc='Close ad' or @content-desc='Collapse' or @text='⌄' or contains(@resource-id, 'close')]"));

            if (!explicitClose.isEmpty()) {
                explicitClose.getFirst().click();
                logStep("[ACTION] Tapped explicit collapse button.");
                Thread.sleep(1500);
            } else {
                var webViews = driver.findElements(AppiumBy.className("android.webkit.WebView"));
                if (!webViews.isEmpty()) {
                    logStep("[ACTION] Ghost WebView detected. Relying on SafeClick Recovery for navigation.");
                }
            }
        } catch (Exception ignored) {}
        finally { driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); }
    }

    // ==========================================
    // PRECISION INPUT & SCROLLING (W3C ACTIONS)
    // ==========================================

    public void performDoubleTap(org.openqa.selenium.WebElement element) {
        try {
            org.openqa.selenium.Dimension size = element.getSize();
            org.openqa.selenium.Point location = element.getLocation();
            int centerX = location.getX() + (size.getWidth() / 2);
            int centerY = location.getY() + (size.getHeight() / 2);
            performDoubleTapCoords(centerX, centerY);
        } catch (Exception e) { logStep("[ERROR] Element double-tap failed."); }
    }

    public void performDoubleTapCoords(int x, int y) {
        logStep("[ACTION] Performing coordinate double-tap at X:" + x + " Y:" + y);
        try {
            org.openqa.selenium.interactions.PointerInput finger = new org.openqa.selenium.interactions.PointerInput(org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tapSequence = new org.openqa.selenium.interactions.Sequence(finger, 1);
            tapSequence.addAction(finger.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
            tapSequence.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tapSequence.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));

            // THE FIX: Micro-delay adjusted to 50ms for lightning-fast double tap
            tapSequence.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(50)));

            tapSequence.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tapSequence.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(tapSequence));
            Thread.sleep(1000);
        } catch (Exception e) { logStep("[ERROR] Coordinate tap failed."); }
    }

    public void performTap(int x, int y) {
        try {
            org.openqa.selenium.interactions.PointerInput finger = new org.openqa.selenium.interactions.PointerInput(org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap = new org.openqa.selenium.interactions.Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(tap));
        } catch (Exception ignored) {}
    }

    public void scrollVertical(boolean scrollViewDown) {
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int startX = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * (scrollViewDown ? 0.7 : 0.3));
            int endY = (int) (size.getHeight() * (scrollViewDown ? 0.3 : 0.7));
            performSwipe(startX, startY, startX, endY);
            Thread.sleep(1000);
        } catch (Exception ignored) {}
    }

    public void scrollHorizontal(int yCoordinate, boolean swipeViewRight) {
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int startX = (int) (size.getWidth() * (swipeViewRight ? 0.8 : 0.2));
            int endX = (int) (size.getWidth() * (swipeViewRight ? 0.2 : 0.8));
            performSwipe(startX, yCoordinate, endX, yCoordinate);
            Thread.sleep(1000);
        } catch (Exception ignored) {}
    }

    private void performSwipe(int startX, int startY, int endX, int endY) {
        org.openqa.selenium.interactions.PointerInput finger = new org.openqa.selenium.interactions.PointerInput(org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe = new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.Arrays.asList(swipe));
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
        // COMMENTED OUT FOR DEBUGGING: Prevents app wipe if test crashes!
        // clearAppData();
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