package com.project.tests;

import com.project.base.BaseTest;
import com.project.pages.FTUEPage;
import com.project.pages.LanguagePage;
import com.project.pages.SplashPage;
import com.project.pages.SubscriptionPage;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OnboardingTest extends BaseTest {

    @Test
    public void masterOnboardingFlowTest() throws Exception {
        SoftAssert softAssert = new SoftAssert();
        LanguagePage langPage = new LanguagePage(driver);
        FTUEPage ftuePage = new FTUEPage(driver);
        SubscriptionPage subPage = new SubscriptionPage(driver);

        By adBannerLocator = AppiumBy.xpath("//androidx.compose.ui.viewinterop.ViewFactoryHolder | //*[contains(@resource-id, 'ad') or contains(@resource-id, 'banner') or contains(@resource-id, 'mys-creative')]");

        logStep("\n========== STARTING MASTER ONBOARDING FLOW ==========");

        logStep("[STEP 1] Turning OFF Wi-Fi and Data (App is not running yet)...");
        setNetworkState(false);

        logStep("[STEP 2] Booting app offline for the first time, expecting 'No Internet' prompt...");
        driver.activateApp(appPackage);

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='No Internet']")));
            logStep("✅ 'No Internet' screen successfully verified.");
        } catch (Exception e) {
            reportBug("'No Internet' screen did not appear within 15 seconds!", "Missing_No_Internet_Screen");
        }

        logStep("[STEP 3] Turning ON Wi-Fi and Data...");
        setNetworkState(true);

        logStep("[ACTION] Waiting 5 seconds for the app to auto-refresh and load UI...");
        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

        logStep("[TEST] Verifying if ad failed to load on fresh online start...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (driver.findElements(adBannerLocator).isEmpty()) {
            reportBug("Ad failed to load upon reconnecting to internet.", "Missed_Ad_On_Reconnect");
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        logStep("[STEP 4] Waiting for Language Settings UI...");
        softAssert.assertTrue(langPage.isLanguageScreenLoaded(), "Language UI failed to load.");

        logStep("[STEP 5] Switching languages to test UI responsiveness...");
        langPage.selectLanguage("Hindi", true);
        langPage.selectLanguage("Korean", true);

        logStep("[ACTION] Scrolling to bottom...");
        langPage.selectLanguage("Spanish", true);

        logStep("[ACTION] Scrolling back to top...");
        langPage.selectLanguage("English", false);

        logStep("[STEP 6] Testing In-App Back Button ('<-')...");
        langPage.clickInAppBackButton();
        langPage.clickInAppBackButton();
        if (langPage.isLanguageScreenLoaded()) {
            reportBug("In-app back button is NON-FUNCTIONAL. No exit prompt appeared.", "Broken_InApp_BackButton");
        }

        logStep("[STEP 7] Tapping Blue Tick to proceed...");
        langPage.clickDone();

        for (int i = 1; i <= 3; i++) {
            logStep("[STEP " + (7+i) + "] FTUE Page " + i + " Ad Verification...");
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            if (driver.findElements(adBannerLocator).isEmpty()) {
                reportBug("No ad banner displayed on FTUE Page " + i, "Missing_Ad_FTUE_Page_" + i);
            }
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            if (i < 3) {
                ftuePage.clickNext();
            } else {
                ftuePage.clickFinish();
            }
        }

        logStep("[STEP 11] Subscription UI loaded. Testing Tier Selections...");
        subPage.selectAllTiers();
        subPage.clickContinue();

        logStep("[STEP 12] Handling Google Play Payment UI...");
        subPage.closeGooglePlayOverlay();

        logStep("[STEP 13] Verifying 'Restore Purchases' functionality...");
        subPage.clickRestorePurchases();
        try {
            Thread.sleep(2000);
            if (!driver.findElements(AppiumBy.xpath("//*[@text='Restore purchases']")).isEmpty()) {
                reportBug("'Restore purchases' button is completely NON-FUNCTIONAL.", "Broken_Restore_Purchases");
            }
        } catch (Exception ignored) {}

        logStep("[STEP 14] Closing Subscription UI...");
        subPage.closeSubscriptionUI();

        logStep("[STEP 15] Checking Home Screen for Ad Banner...");
        softAssert.assertTrue(!driver.findElements(AppiumBy.xpath("//*[@text='Stone Identifier']")).isEmpty(), "Home Tab did not load.");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (driver.findElements(adBannerLocator).isEmpty()) {
            reportBug("No ad banner displayed on Home Screen.", "Missing_Ad_Home_Screen");
        } else {
            logStep("✅ Ad banner successfully displayed on Home Screen.");
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        logStep("[STEP 16] Test Flow Complete. Initiating Cleanup...");

        logStep("[STEP 17] Pressing Device BACK button from Home Tab...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(2000);

        if (driver.findElements(AppiumBy.xpath("//*[contains(@text, 'sure') or contains(@text, 'Exit') or contains(@text, 'Quit')]")).isEmpty()) {
            reportBug("Prompt not displayed before killing the app.", "Missing_Exit_Prompt");
        }

        logStep("[ACTION] Relaunching the app for Warm Start full-screen ad test...");
        driver.activateApp(appPackage);
        closeInterstitialAds();

        logStep("[ACTION] Handling Subscription UI after warm start...");
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue']")).isEmpty()) {
            subPage.closeSubscriptionUI();
        }

        logStep("[STEP 18] Tapping 'Identify' button...");
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify' or @text='Identify a Rock']"), "Scan/Identify Button");

        logStep("[ACTION] Tapping 'Get Premium'...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Get Premium']"))).click();
        subPage.closeSubscriptionUI();

        logStep("[ACTION] Tapping 'Watch Ad for 1 Use'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();

        closeInterstitialAds();

        logStep("[ACTION] Testing Camera Permission Flow...");
        By grantBtn = AppiumBy.xpath("//*[@text='Grant Permission']");
        By osDenyBtn = AppiumBy.id("com.android.permissioncontroller:id/permission_deny_button");
        By osDenyDontAskBtn = AppiumBy.id("com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button");

        wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();
        wait.until(ExpectedConditions.elementToBeClickable(osDenyBtn)).click();

        wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();
        wait.until(ExpectedConditions.elementToBeClickable(osDenyDontAskBtn)).click();

        logStep("[TEST] Verifying App Soft Lock State...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(1500);
        driver.findElement(grantBtn).click();
        Thread.sleep(1500);

        if (!driver.findElements(grantBtn).isEmpty()) {
            reportBug("App soft-locked. User is unable to close the prompt and go back.", "Camera_Permission_SoftLock");
        }

        logStep("[SYSTEM] ADB 'pm grant' fails on MIUI. Teleporting to App Settings...");
        driver.executeScript("mobile: shell", java.util.Map.of("command", "am", "args", java.util.Arrays.asList("start", "-a", "android.settings.APPLICATION_DETAILS_SETTINGS", "-d", "package:" + appPackage)));

        System.out.println("\n====================================================================");
        System.out.println("[ACTION REQUIRED] 🛑 MANUAL PERMISSION GRANT 🛑");
        System.out.println("1. Look at your phone: The App Settings page should be open.");
        System.out.println("2. Tap 'App permissions' -> 'Camera' -> 'Allow'.");
        System.out.println("3. Use the Android Recent Apps button to return to the Stone App.");
        System.out.println("4. Once you are back inside the Stone app, press [ENTER] here.");
        System.out.println("====================================================================\n");

        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] User returned to app. Engaging AdBuster to clear the 'Welcome Back' ad...");
        closeInterstitialAds();

        System.out.println("\n====================================================================");
        System.out.println("[STEP 19] 🛑 MANUAL PHOTO REQUIRED 🛑");
        System.out.println("1. The Camera UI should now be active and completely ad-free.");
        System.out.println("2. Please manually take a photo of a stone (or anything).");
        System.out.println("3. Wait for the app to process and load the 'Crop Stone' UI.");
        System.out.println("4. Once you see the Rotate and DONE buttons, press [ENTER] here.");
        System.out.println("====================================================================\n");

        new java.util.Scanner(System.in).nextLine();
        logStep("[SYSTEM] User confirmed Crop UI loaded.");

        // =========================================================
        // STEP 20: CROP UI VOLATILITY & COLLECTION MANAGEMENT
        // =========================================================
        logStep("[STEP 20] Testing Crop UI Offline Volatility...");
        setNetworkState(false);

        logStep("[ACTION] Tapping 'DONE' while offline...");
        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        try { wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[contains(@text, 'Internet') or contains(@text, 'network') or contains(@text, 'connection')]"))); } catch (Exception ignored) {}
        setNetworkState(true);
        Thread.sleep(3000);

        if (driver.findElements(AppiumBy.xpath("//*[@text='DONE']")).isEmpty()) {
            reportBug("Clicked photo failed to be retained after turning the internet back on. Camera UI re-opened.", "Volatile_Memory_Loss_CropUI");
        }

        System.out.println("\n====================================================================");
        System.out.println("[ACTION REQUIRED] 🛑 SECOND MANUAL PHOTO REQUIRED 🛑");
        System.out.println("1. The photo was lost. Please manually take ANOTHER photo.");
        System.out.println("2. Wait for the 'Crop Stone' UI to load again.");
        System.out.println("3. Once you see the Rotate and DONE buttons, press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] Tapping rotate icon twice (0.5s delay)...");
        org.openqa.selenium.WebElement rotateBtn = driver.findElement(AppiumBy.xpath("//*[@content-desc='Rotate']"));
        rotateBtn.click(); Thread.sleep(500); rotateBtn.click(); Thread.sleep(500);

        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        logStep("[SYSTEM] Processing image. Engaging AdBuster for post-analysis ads...");
        closeInterstitialAds();

        logStep("[TEST] Extracting analyzed stone data...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@content-desc='Save Rock']")));

        String analyzedStoneName = "Diamond";
        try { analyzedStoneName = driver.findElement(AppiumBy.xpath("//*[@text='Gemstone']/../preceding-sibling::android.widget.TextView[1]")).getText(); } catch (Exception ignored) {}
        logStep("-> Analyzed Stone Identified As: " + analyzedStoneName);

        logStep("[ACTION] Bookmarking stone and returning to Home...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Save Rock']")).click();
        Thread.sleep(1000);
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back']")).click();

        collapseHalfScreenAd();

        logStep("[ACTION] Navigating to 'My Rocks'...");
        safeClick(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"), "My Rocks Tab");

        logStep("[TEST] Verifying '1 Gem Found' and checking dynamic stone name...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='1 Gem Found' or @text='My Collection']")));

        if (driver.findElements(AppiumBy.xpath("//android.widget.TextView[@text='" + analyzedStoneName + "']")).isEmpty()) {
            reportBug("Analyzed stone (" + analyzedStoneName + ") was not saved to My Collection.", "Failed_Bookmark_Write");
        } else {
            logStep("✅ " + analyzedStoneName + " successfully verified in My Collection.");
        }

        logStep("[TEST] Dropping network on Collection UI...");
        setNetworkState(false);
        setNetworkState(true);

        closeInterstitialAds();
        collapseHalfScreenAd();

        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='My Collection']")).isEmpty()) {
            logStep("[ACTION] App forced Home Tab. Navigating back to My Rocks...");
            safeClick(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"), "My Rocks Tab");
        }

        logStep("[TEST] Verifying data persistence after reconnect...");
        if (driver.findElements(AppiumBy.xpath("//android.widget.TextView[@text='" + analyzedStoneName + "']")).isEmpty()) {
            reportBug("Collection data lost after network reconnect.", "Collection_Data_Wipe");
        }

        logStep("[ACTION] Deleting the rock...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Delete']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Delete']"))).click();
        Thread.sleep(2500);

        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='My Collection']")).isEmpty()) {
            reportBug("App unexpectedly navigated to Home tab after deletion instead of staying on My Collection UI.", "Delete_Routing_Error");
        }

        // =========================================================
        // STEP 21: CRITICAL RECOVERY & PERSISTENCE STRESS TESTS
        // =========================================================
        logStep("\n========== STARTING STEP 21: CRITICAL PATH STRESS TESTS ==========");

        logStep("[ACTION] Re-navigating to 'My Rocks'...");
        if (driver.findElements(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']")).isEmpty()) {
            safeClick(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']"), "Home Tab");
            safeClick(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"), "My Rocks Tab");
        }

        collapseHalfScreenAd();

        logStep("[ACTION] Tapping 'Identify' via Safe-Zone Bottom Nav...");
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify a Rock' or @text='Identify']"), "Scan/Identify Button");

        logStep("[ACTION] Tapping 'Watch Ad for 1 Use'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();
        closeInterstitialAds();

        logStep("[ACTION] Testing Capture Double-Tap race condition...");
        org.openqa.selenium.WebElement captureBtn = null;
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            captureBtn = driver.findElement(AppiumBy.xpath("//*[@content-desc='Capture' or @content-desc='Shutter' or @content-desc='Take photo']"));
        } catch (Exception ignored) {}
        finally { driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); }

        if (captureBtn != null) {
            performDoubleTap(captureBtn);
        } else {
            logStep("[WARNING] Camera button not natively identifiable. Firing W3C coordinate Double-Tap at bottom center...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            performDoubleTapCoords(size.getWidth() / 2, (int) (size.getHeight() * 0.85));
        }

        logStep("[ACTION] Tapping 'DONE' on the Crop Stone UI...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='DONE']"))).click();
        Thread.sleep(1500);

        if (!driver.findElements(AppiumBy.xpath("//*[@text='DONE']")).isEmpty()) {
            reportBug("2 images captured on double pressing the capture button instead of just 1.", "RaceCondition_DoubleCapture");
            logStep("[ACTION] Clearing the 1st stacked Crop UI...");
            driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();
        }

        // THE FIX: Catch the ad that plays WHILE it processes the "Not a Stone" image
        logStep("[SYSTEM] Processing image. Engaging AdBuster for post-analysis ads...");
        closeInterstitialAds();
        dismissPopups();

        logStep("[SYSTEM] Checking results (expecting 'Not a Stone' AI failure)...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Not a Stone'] | //*[@content-desc='Retry' or contains(@content-desc, 'Try')]")));

        logStep("[ACTION] Tapping 'Try Again' and verifying navigation routing...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Try Again'] | //*[@content-desc='Retry' or contains(@content-desc, 'Try')]"))).click();
        Thread.sleep(2000);

        if (driver.findElements(AppiumBy.xpath("//*[@text='Discover the Mystery' or contains(@text, 'Mystery')]")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='IdentifyYourRock'] | //*[@content-desc='Capture']")).isEmpty()) {
            reportBug("App failed to redirect the user to the stone capture/Discover UI. User incorrectly routed to Home tab.", "FailureRouting_TryAgain_BypassedCamera");
        }

        logStep("[ACTION] Recovering flow: navigating back to Camera UI...");
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() || !driver.findElements(AppiumBy.xpath("//*[@text='Identify a Rock']")).isEmpty()) {
            if (!driver.findElements(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']")).isEmpty()) {
                safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify a Rock' or @text='Identify']"), "Scan Button");
            } else {
                safeClick(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"), "My Rocks Tab");
                safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify a Rock' or @text='Identify']"), "Scan Button");
            }
        }

        logStep("[ACTION] Watching rewarded ad again...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();
        closeInterstitialAds();

        System.out.println("\n====================================================================");
        logStep("[ACTION REQUIRED] 🛑 THIRD MANUAL PHOTO REQUIRED 🛑");
        logStep("1. The Ad is clear. Please manually take a clear photo of a REAL stone.");
        logStep("2. Once you see the Crop Stone DONE button, press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] User confirmed photo taken. Engaging Background/Foreground App Test...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='DONE']"))).click();

        driver.runAppInBackground(Duration.ofSeconds(2));
        logStep("[SYSTEM] App foregrounded. Intercepting Warm Start ad...");
        closeInterstitialAds();
        dismissPopups();

        logStep("[TEST] Verifying if App State was preserved after backgrounding...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        boolean isCameraUIBack = !driver.findElements(AppiumBy.xpath("//*[@content-desc='Capture'] | //*[@content-desc='Shutter'] | //*[@content-desc='Take photo']")).isEmpty();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (isCameraUIBack) {
            reportBug("App state lost after backgrounding during processing. User was incorrectly dropped back to Camera UI.", "StateLoss_Backgrounding");

            logStep("[ACTION] Auto-Recovering: Taking a rapid automated photo to bypass the bug and continue testing...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            // Tap the capture button using coordinates (bottom center)
            performTap(size.getWidth() / 2, (int) (size.getHeight() * 0.85));

            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='DONE']"))).click();
            logStep("[SYSTEM] Processing recovery image...");
            closeInterstitialAds();
            dismissPopups();
        }

        logStep("[TEST] Extracting analyzed stone data (Waiting up to 30s for AI Processing)...");
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@content-desc='Save Rock']")));

        String lastStoneAnalyzed = "Diamond";
        try { lastStoneAnalyzed = driver.findElement(AppiumBy.xpath("//*[@text='Gemstone']/../preceding-sibling::android.widget.TextView[1]")).getText(); } catch (Exception ignored) {}
        logStep("-> Analysis Restored Stone As: " + lastStoneAnalyzed);

        logStep("[ACTION] Bookmarking stone and returning to verify persistence...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Save Rock']")).click();
        Thread.sleep(1000);
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back']")).click();

        logStep("[ACTION] Navigating to 'My Rocks' for persistence verification...");
        safeClick(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"), "My Rocks Tab");

        if (driver.findElements(AppiumBy.xpath("//android.widget.TextView[@text='" + lastStoneAnalyzed + "']")).isEmpty()) {
            reportBug("Missing recently saved stone. Data Persistence Failed after background/foreground ad loop.", "FailedPersistence_WarmStartAd");
        } else {
            logStep("✅ Persistence of dynamic stone (" + lastStoneAnalyzed + ") verified in My Collection UI.");
        }

        logStep("[ACTION] Verification complete. Navigating back to home...");
        safeClick(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']"), "Home Tab");


        // =========================================================
        // STEP 22: PROCESSING INTERRUPTION & CACHE LEAK TEST
        // =========================================================
        logStep("\n========== STARTING STEP 22: PROCESSING INTERRUPTION STRESS TEST ==========");

        collapseHalfScreenAd();
        closeInterstitialAds();

        logStep("[ACTION] Tapping 'Identify' via Safe-Zone Bottom Nav...");
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify' or @text='Identify a Rock']"), "Scan/Identify Button");

        logStep("[ACTION] Tapping 'Watch Ad for 1 Use'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();

        logStep("[ACTION] Watching rewarded ad(s). Checking for trailing extra ads...");
        closeInterstitialAds();

        System.out.println("\n====================================================================");
        logStep("[ACTION REQUIRED] 🛑 FOURTH MANUAL PHOTO REQUIRED 🛑");
        logStep("1. The Ad is clear. Please manually take a photo.");
        logStep("2. Once you see the Crop Stone DONE button, press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] Tapping 'DONE' and interrupting processing with Device Back Button after 0.5s...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='DONE']"))).click();
        Thread.sleep(500);

        logStep("[ACTION] Pressing Device BACK button...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(2000);

        collapseHalfScreenAd();
        closeInterstitialAds();

        logStep("[ACTION] Tapping 'Identify' button again...");
        if(driver.findElements(AppiumBy.xpath("//*[@text='Identify' or @text='Identify a Rock']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@content-desc='Scan']")).isEmpty()) {
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
            Thread.sleep(1000);
        }
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify' or @text='Identify a Rock']"), "Scan/Identify Button");

        logStep("[ACTION] Tapping 'Watch Ad for 1 Use' again...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();

        logStep("[ACTION] Watching rewarded ad(s)...");
        closeInterstitialAds();

        logStep("[TEST] Verifying if Camera UI loaded or Cached Description UI leaked...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        boolean isDescriptionUI = !driver.findElements(AppiumBy.xpath("//*[@content-desc='Save Rock'] | //*[@text='Gemstone'] | //*[contains(@text, 'Description')]")).isEmpty();
        boolean isCameraUI = !driver.findElements(AppiumBy.xpath("//*[@content-desc='Capture'] | //*[@content-desc='Shutter'] | //*[@content-desc='Take photo']")).isEmpty();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (isDescriptionUI) {
            reportBug("App failed to open the camera/capture UI and instead loaded the previously captured stone description UI.", "CacheLeak_DescriptionUI_Loaded");
            logStep("[ACTION] Bug found. Navigating back to Home...");
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
        } else if (isCameraUI) {
            logStep("✅ Camera UI opened as intended. No cache leak detected.");
            logStep("[ACTION] Navigating back to Home...");
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        } else {
            logStep("[WARNING] Neither Camera nor Description UI clearly detected. Pressing back to recover...");
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        }

        collapseHalfScreenAd();
        closeInterstitialAds();


        // =========================================================
        // STEP 23: SEARCH, ROUTING, & HORIZONTAL CAROUSEL TESTS
        // =========================================================
        logStep("\n========== STARTING STEP 23: SEARCH & NAVIGATION ROUTING ==========");

        collapseHalfScreenAd();

        logStep("[ACTION] Scrolling to 'Discover More Stones'...");
        boolean foundDiscoverBtn = false;
        for (int i = 0; i < 5; i++) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
            var discoverBtn = driver.findElements(AppiumBy.xpath("//*[@text='Discover More Stones' or contains(@text, 'Discover More')]"));
            if (!discoverBtn.isEmpty() && discoverBtn.getFirst().isDisplayed()) {
                discoverBtn.getFirst().click();
                foundDiscoverBtn = true;
                break;
            }
            scrollVertical(true); // Scroll View Down
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (!foundDiscoverBtn) reportBug("Failed to find 'Discover More Stones' button on Home tab.", "Missing_Discover_Button");

        closeInterstitialAds();

        logStep("[TEST] Verifying Discover Stones UI and Negative Search state...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Discover Stones']")));

        org.openqa.selenium.WebElement searchBox = driver.findElement(AppiumBy.className("android.widget.EditText"));
        searchBox.click();
        searchBox.sendKeys("ycyc");

        try { driver.hideKeyboard(); } catch (Exception ignored) { logStep("[WARNING] Native keyboard not detected to hide."); }

        if (driver.findElements(AppiumBy.xpath("//*[@text='No stones found']")).isEmpty()) {
            reportBug("Error message 'No stones found' not displayed for invalid search.", "Missing_Search_Error");
        }

        logStep("[ACTION] Clearing search and entering valid stone (Lapis Lazuli)...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Clear search']")).click();

        searchBox = driver.findElement(AppiumBy.className("android.widget.EditText"));
        searchBox.sendKeys("Lapis Lazuli");
        try { driver.hideKeyboard(); } catch (Exception ignored) {}

        logStep("[ACTION] Tapping search result...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Lapis Lazuli'] | //*[@content-desc='Lapis Lazuli']"))).click();

        closeInterstitialAds();

        logStep("[TEST] Verifying Description UI and Back Navigation Routing...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Description' or contains(@text, 'Description')]")));

        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
        Thread.sleep(2000);

        if (driver.findElements(AppiumBy.xpath("//*[@text='Discover Stones']")).isEmpty()) {
            reportBug("User navigated to the Home tab instead of the Discover Stones page.", "RoutingError_DiscoverStones_Back");
        } else {
            logStep("✅ Correctly returned to Discover Stones UI. Navigating back to Home...");
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
        }

        logStep("\n========== STARTING HORIZONTAL CAROUSEL TEST ==========");
        closeInterstitialAds();
        collapseHalfScreenAd();

        logStep("[ACTION] Locating 'Popular Rocks' or 'Zodiac Gemstones' category...");
        org.openqa.selenium.WebElement categoryHeader = null;
        for (int i = 0; i < 5; i++) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
            var headers = driver.findElements(AppiumBy.xpath("//*[@text='Popular Rocks' or @text='Zodiac Gemstones']"));
            if (!headers.isEmpty() && headers.getFirst().isDisplayed()) {
                categoryHeader = headers.getFirst();
                break;
            }
            scrollVertical(true);
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (categoryHeader != null) {
            logStep("[ACTION] Found category: " + categoryHeader.getText() + ". Performing horizontal swipe...");
            int carouselY = categoryHeader.getLocation().getY() + 250;

            scrollHorizontal(carouselY, true);

            logStep("[ACTION] Tapping an element inside the horizontally scrolled list...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int tapX = (int) (size.getWidth() * 0.7);
            performTap(tapX, carouselY);

            closeInterstitialAds();

            logStep("[TEST] Verifying Description page loaded from horizontal list...");
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Description' or contains(@text, 'Description')]")));
                logStep("✅ Successfully loaded description from carousel.");
                driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
            } catch (Exception e) {
                reportBug("Failed to load stone description after clicking element in horizontal carousel.", "Broken_Carousel_Link");
            }
        } else {
            reportBug("Failed to locate 'Popular Rocks' or 'Zodiac Gemstones' on Home Tab.", "Missing_Horizontal_Categories");
        }

        System.out.println("\n===============================================");
        logStep("🎉 STEP 23 COMPLETE. THE STONE IDENTIFIER SUITE IS FINISHED. 🎉");
        logStep("Total Bugs Logged: " + totalBugCount);
        System.out.println("===============================================\n");

        softAssert.assertAll();
        org.testng.Assert.assertEquals(totalBugCount, 0, "Suite completed, but " + totalBugCount + " bugs were found! Check the HTML report.");
    }
}