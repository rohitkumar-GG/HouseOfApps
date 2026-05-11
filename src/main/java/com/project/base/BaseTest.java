// File: src/main/java/com/project/base/BaseTest.java
package com.project.base;

import com.project.utils.*;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

public class BaseTest {
    protected AndroidDriver driver;
    protected String appPackage;
    protected WebDriverWait wait;

    @BeforeMethod
    public void setupDriver() throws Exception {
        ReportManager.resetStepCounters();
        String appTarget = System.getProperty("targetApp", "Stone");

        if (appTarget.equalsIgnoreCase("Coin")) {
            appPackage = "app.coinidentifier.checker.scanner";
        } else if (appTarget.equalsIgnoreCase("FileRecovery")) {
            appPackage = "com.filerecovery.photovideo.restore";
        } else {
            appPackage = "rock.identifier.diamond.gem.stone.mineral.finder.scanner";
        }

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("83fc08ef");
        options.setNoReset(true);
        options.setCapability("appium:autoLaunch", false);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);
        options.setCapability("appium:ignoreUnimportantViews", true);
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:allowInvisibleElements", true);
        options.setCapability("appium:enableMultiWindows", true);
        options.setCapability("appium:waitForIdleTimeout", 100);
        options.setCapability("appium:newCommandTimeout", 3600);
        options.setAppPackage(appPackage);
        options.setAppActivity(appPackage + ".MainActivity");

        // Connects to the server spun up by MenuRunner
        driver = new AndroidDriver(AppiumServerManager.getServiceUrl(), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ==========================================
    // WRAPPER METHODS
    // ==========================================
    public void logStep(String message) { ReportManager.logStep(message); }
    public void reportBug(String msg, String img) { ReportManager.reportBug(driver, msg, img); }
    public void beginTestStep(String stepName, String description) { ReportManager.beginTestStep(stepName, description); }
    public void endTestStep() { ReportManager.endTestStep(); }
    public void setNetworkState(boolean state) { SystemUtils.setNetworkState(driver, state); }
    public void collapseHalfScreenAd() { AdManager.collapseHalfScreenAd(driver); }
    public void closeInterstitialAds() { AdManager.closeInterstitialAds(driver, appPackage); }
    public void clearAppData() { SystemUtils.clearAppData(driver, appPackage); }
    public void scrollVertical(boolean down) { GestureUtils.scrollVertical(driver, down); }
    public void scrollHorizontal(int y, boolean right) { GestureUtils.scrollHorizontal(driver, y, right); }
    public void performTap(int x, int y) { GestureUtils.performTap(driver, x, y); }
    public void performDoubleTap(org.openqa.selenium.WebElement e) { GestureUtils.performDoubleTap(driver, e); }
    public void performDoubleTapCoords(int x, int y) { GestureUtils.performDoubleTapCoords(driver, x, y); }

    public void returnToHomeTab() {
        logStep("[ACTION] Enforcing navigation back to Home Tab...");
        collapseHalfScreenAd();

        for (int i = 0; i < 5; i++) {
            boolean isHome = !driver.findElements(AppiumBy.xpath(
                    "//*[@text='Identify rocks accurately in seconds'] | //*[@text='Discover More Stones'] | //*[@text='Popular Rocks'] | //*[@text='What’s this rock?']"
            )).isEmpty();

            if (isHome) {
                logStep("✅ Successfully verified Home Tab is active.");
                return;
            }

            var homeTabBtn = driver.findElements(AppiumBy.xpath("//*[@content-desc='Home' or @text='Home']"));
            if (!homeTabBtn.isEmpty() && homeTabBtn.getFirst().isDisplayed()) {
                logStep("   -> Bottom Nav detected. Tapping Home Tab via Coordinate Strike...");
                try {
                    org.openqa.selenium.Point loc = homeTabBtn.getFirst().getLocation();
                    org.openqa.selenium.Dimension size = homeTabBtn.getFirst().getSize();
                    performTap(loc.getX() + (size.getWidth() / 2), loc.getY() + (size.getHeight() / 2));
                    Thread.sleep(1500);
                } catch (Exception ignored) {}

                boolean tapWorked = !driver.findElements(AppiumBy.xpath("//*[@text='Popular Rocks'] | //*[@text='What’s this rock?']")).isEmpty();
                if (!tapWorked) {
                    logStep("   -> Tab switch swallowed by Compose UI. Firing safe Accessibility BACK swipe to route Home...");
                    driver.navigate().back();
                    try { Thread.sleep(1500); } catch (Exception ignored) {}
                }
                continue;
            }

            logStep("   -> Sub-menu detected. Attempting to navigate back safely...");
            var uiBackButton = driver.findElements(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']"));
            if (!uiBackButton.isEmpty() && uiBackButton.getFirst().isDisplayed()) {
                uiBackButton.getFirst().click();
            } else {
                driver.navigate().back();
            }
            try { Thread.sleep(1500); } catch (Exception ignored) {}
        }
    }

    public void safeClick(By locator, String elementName) {
        logStep("[ACTION] Safe-clicking: " + elementName);
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            org.openqa.selenium.WebElement element = driver.findElement(locator);
            element.click();
            Thread.sleep(1500);

            String currentPkg = driver.getCurrentPackage();
            if (currentPkg != null && !currentPkg.equals(appPackage)) {
                boolean isLauncher = currentPkg.contains("launcher") || currentPkg.contains("trebuchet") || currentPkg.contains("nexus");

                if (isLauncher) {
                    logStep("⚠️ App dropped to home screen! (Hit OS gesture bar). Recovering...");
                    driver.activateApp(appPackage);
                    Thread.sleep(2000);
                } else {
                    logStep("⚠️ Ad Intercepted! Opened Play Store/Browser (" + currentPkg + "). Recovering...");
                    driver.navigate().back();
                    Thread.sleep(2000);
                    if (!driver.getCurrentPackage().equals(appPackage)) {
                        driver.activateApp(appPackage);
                        Thread.sleep(2000);
                    }
                }
                collapseHalfScreenAd();
                element = driver.findElement(locator);
                element.click();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logStep("[WARNING] Safe-click element not found/stale: " + elementName);
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }

    public void dismissPopups() {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            var popups = driver.findElements(AppiumBy.xpath(
                    "//android.widget.TextView[@text='Not Now' or @text='Cancel' or @text='No Thanks' or @text='Later' or @text='Decline'] | " +
                            "//android.widget.Button[@text='Not Now' or @text='Cancel' or @text='No Thanks' or @text='Later' or @text='Decline'] | " +
                            "//*[@resource-id='close_btn' or @resource-id='dismiss']"
            ));

            if (!popups.isEmpty()) {
                popups.getFirst().click();
                Thread.sleep(1000);
            }
        } catch (Exception ignored) {}
        finally { driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); }
    }

    public void dismissCashPaymentPopup() {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            var cashPopupTitle = driver.findElements(AppiumBy.xpath("//*[@text='Want to pay with cash?']"));
            if (!cashPopupTitle.isEmpty()) {
                var okButton = driver.findElements(AppiumBy.xpath("//android.widget.Button[@text='OK' or @text='Ok']"));
                if (!okButton.isEmpty()) {
                    okButton.getFirst().click();
                    Thread.sleep(1500);
                }
            }
        } catch (Exception ignored) {}
        finally { driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); }
    }
}