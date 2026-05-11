// File: src/main/java/com/project/utils/AdManager.java
package com.project.utils;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Dimension;
import java.time.Duration;

public class AdManager {

    @SuppressWarnings("BusyWait")
    public static void closeInterstitialAds(AndroidDriver driver, String appPackage) {
        ReportManager.logStep("[ACTION] AdBuster Engaged: Quick-scanning for Ads or Target UI...");
        long endTime = System.currentTimeMillis() + 120000;
        long adBusterStartTime = System.currentTimeMillis();

        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

        try {
            while (System.currentTimeMillis() < endTime) {
                if (checkAndRecoverHijack(driver, appPackage)) continue;

                long elapsedTime = System.currentTimeMillis() - adBusterStartTime;

                boolean isFullScreenWebView = false;
                var webViews = driver.findElements(AppiumBy.className("android.webkit.WebView"));
                if (!webViews.isEmpty()) {
                    try {
                        int wvHeight = webViews.getFirst().getSize().getHeight();
                        int screenHeight = driver.manage().window().getSize().getHeight();
                        if (wvHeight > (screenHeight * 0.5)) isFullScreenWebView = true;
                    } catch (Exception ignored) {}
                }

                boolean hasAdIndicators = !driver.findElements(AppiumBy.xpath(
                        "//*[contains(@text, 'Reward')] | //*[starts-with(@text, 'Ad ')] | " +
                                "//*[contains(@text, 'Skip ad')] | //*[@resource-id='ad_container'] | " +
                                "//*[@resource-id='video_box'] | //*[@text='Learn More'] | " +
                                "//*[@text='Install'] | //*[@text='Open'] | //*[@text='Play']"
                )).isEmpty();

                boolean isSafeUiVisible = !driver.findElements(AppiumBy.xpath(
                        "//*[contains(@content-desc, 'Capture')] | " +
                                "//*[contains(@text, 'STEP 1') or contains(@text, 'Step 1')] | " +
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
                                "//*[@text='Not a Stone'] | " +
                                "//*[@text='Discover the Mystery'] | " +
                                "//*[@text='Try Again']"
                )).isEmpty();

                var adCounters = driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Ad ')] | //*[contains(@text, 'Reward')]"));
                boolean isCountingDown = false;
                if (!adCounters.isEmpty()) {
                    String statusText = adCounters.getFirst().getText();
                    if (statusText.matches(".*\\d+.*")) isCountingDown = true;
                }

                if (isSafeUiVisible && !isFullScreenWebView && !isCountingDown) {
                    ReportManager.logStep("[ACTION] Target UI detected and unblocked. Ignoring immersive ads...");
                    return;
                }

                String skipXPath = "//*[@text='Skip' or @text='SKIP' or contains(@text, 'Skip ad') or contains(@text, 'Skip video') or contains(@content-desc, 'Skip') or contains(@resource-id, 'skip') or @text='Continue']";
                var skipBtns = driver.findElements(AppiumBy.xpath(skipXPath));

                if (!skipBtns.isEmpty()) {
                    skipBtns.getFirst().click();
                    ReportManager.logStep("[ACTION] Tapped 'Skip' button to fast-forward video ad.");
                    Thread.sleep(1500);
                    continue;
                }

                if (isCountingDown) {
                    Thread.sleep(1000);
                    continue;
                }

                String closeXPath = "//*[@content-desc='Close' or @content-desc='close' or @text='Close' or @text='X' or contains(@resource-id, 'close') or contains(@resource-id, 'dismiss') or (contains(@text, 'Continue') and contains(@text, 'app'))] | " +
                        "//*[contains(@text, 'Reward')]/..//android.widget.Image | //*[contains(@text, 'Reward')]/following-sibling::* | " +
                        "//*[@resource-id='video_container']//android.widget.Button | " +
                        "//*[@resource-id='ad_container']//android.widget.Button | " +
                        "//*[@resource-id='app-interstitial-slot']//android.widget.Button | " +
                        "//*[@resource-id='collapse-expand-button-root'] | " +
                        "//*[@resource-id='collapse-expand-button'] | " +
                        "//*[@content-desc='Close ad' or @content-desc='Collapse' or @text='⌄']";

                var closeBtns = driver.findElements(AppiumBy.xpath(closeXPath));

                if (!closeBtns.isEmpty()) {
                    closeBtns.getFirst().click();
                    ReportManager.logStep("[ACTION] Tapped an Ad Close/X button.");
                    Thread.sleep(1500);

                    var confirmPopups = driver.findElements(AppiumBy.xpath("//*[@text='Close video' or @text='CLOSE VIDEO' or @text='Close ad' or @text='CLOSE']"));
                    if (!confirmPopups.isEmpty()) {
                        confirmPopups.getFirst().click();
                        Thread.sleep(1500);
                    }
                } else if (isFullScreenWebView || hasAdIndicators) {
                    if (elapsedTime > 5000) {
                        ReportManager.logStep("[ACTION] Ad active for 5s without standard close button. Firing Sweeping Coordinate Strikes...");
                        Dimension size = driver.manage().window().getSize();
                        GestureUtils.performTap(driver, size.getWidth() - 50, 160);
                        Thread.sleep(2000);

                        if (checkAndRecoverHijack(driver, appPackage)) continue;

                        if (!driver.findElements(AppiumBy.className("android.webkit.WebView")).isEmpty()) {
                            GestureUtils.performTap(driver, size.getWidth() - 250, 140);
                            Thread.sleep(2000);
                        }
                    } else {
                        Thread.sleep(1000);
                    }
                } else {
                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            ReportManager.logStep("[WARNING] AdBuster Error: " + e.getMessage());
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }

    public static void collapseHalfScreenAd(AndroidDriver driver) {
        ReportManager.logStep("[ACTION] Checking for native half-screen ad banner(s)...");
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));

            String megaLocator = "//android.widget.LinearLayout[@content-desc='Close button'] | " +
                    "//*[@resource-id='collapse-expand-button-root'] | " +
                    "//*[@resource-id='collapse-expand-button'] | " +
                    "//*[@content-desc='Close ad' or @content-desc='Close' or @content-desc='Collapse' or @text='⌄'] | " +
                    "//*[contains(@resource-id, 'close') or contains(@resource-id, 'dismiss') or contains(@resource-id, 'cancel')]";

            for (int i = 0; i < 2; i++) {
                var elements = driver.findElements(AppiumBy.xpath(megaLocator));

                if (!elements.isEmpty()) {
                    try {
                        elements.getFirst().click();
                        ReportManager.logStep("✅ [SUCCESS] Tapped half-screen close button (Layer " + (i + 1) + ").");
                        Thread.sleep(1500);
                        continue;
                    } catch (Exception e) {
                        ReportManager.logStep("⚠️ [WARNING] Mega-XPath click intercepted.");
                    }
                } else {
                    if (i == 0) {
                        var continueBtn = driver.findElements(AppiumBy.xpath("//*[@text='Continue' or @text='Continue to App']"));
                        if(!continueBtn.isEmpty()) {
                            continueBtn.getFirst().click();
                            ReportManager.logStep("✅ [SUCCESS] Tapped 'Continue' to bypass half-screen ad.");
                            Thread.sleep(1500);
                        }
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
        finally { driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); }
    }

    public static boolean checkAndRecoverHijack(AndroidDriver driver, String appPackage) {
        try {
            String currentPkg = driver.getCurrentPackage();
            if (currentPkg != null && !currentPkg.equals(appPackage) && !currentPkg.equals("com.android.permissioncontroller")) {
                boolean isLauncher = currentPkg.contains("launcher") || currentPkg.contains("trebuchet") || currentPkg.contains("nexus");
                if (isLauncher) {
                    ReportManager.logStep("⚠️ App unexpectedly minimized! (Dropped to OS Launcher). Restoring app...");
                } else {
                    ReportManager.logStep("⚠️ Ad Hijack Intercepted! (Trapped in: " + currentPkg + "). Forcing OS-level return...");
                }
                driver.activateApp(appPackage);
                Thread.sleep(2500);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}