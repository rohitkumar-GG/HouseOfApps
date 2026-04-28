package com.project.tests;

import com.project.base.BaseTest;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class CoinMasterTest extends BaseTest {

    @Test
    public void masterCoinFlowTest() throws Exception {
        SoftAssert softAssert = new SoftAssert();
        By adBannerLocator = AppiumBy.xpath("//androidx.compose.ui.viewinterop.ViewFactoryHolder | //*[contains(@resource-id, 'ad') or contains(@resource-id, 'banner')]");

        logStep("\n========== STARTING COIN IDENTIFIER MASTER FLOW ==========");

        // =========================================================
        // TEST 1: OFFLINE BOOT & RECOVERY
        // =========================================================
        logStep("[TEST 1] Turning OFF Wi-Fi/Data and booting app offline...");
        setNetworkState(false);
        driver.activateApp(appPackage);

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Retry' or @text='No Internet']")));
            logStep("✅ Offline 'Retry/No Internet' prompt successfully verified.");
        } catch (Exception e) {
            reportBug("Offline error screen did not appear within 15 seconds!", "Missing_Offline_Screen");
        }

        logStep("[ACTION] Restoring Network...");
        setNetworkState(true);
        Thread.sleep(5000); // Allow UI to refresh

        logStep("[TEST] Verifying Ad loads on reconnect...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (driver.findElements(adBannerLocator).isEmpty()) {
            reportBug("Ad failed to load upon reconnecting to internet.", "Missed_Ad_On_Reconnect");
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // =========================================================
        // TEST 2: LANGUAGE UI & BACK BUTTON BUG
        // =========================================================
        logStep("[TEST 2] Processing Language UI...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Scroll to find languages (Mocking the action)
        scrollVertical(true);
        scrollVertical(false);

        logStep("[ACTION] Testing In-App Back Button ('<-')...");
        try {
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Back' or @content-desc='Navigate up']")).click();
            Thread.sleep(1000);
            if (!driver.findElements(AppiumBy.xpath("//*[@text='Language' or contains(@text, 'English')]")).isEmpty()) {
                reportBug("In-app back button is NON-FUNCTIONAL. No exit prompt appeared.", "Broken_InApp_BackButton");
            }
        } catch (Exception ignored) {}

        logStep("[ACTION] Tapping Tick/Done to proceed...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Done' or @text='✓' or contains(@resource-id, 'done')] | //android.widget.ImageView[last()]")).click();

        // =========================================================
        // TEST 3: FTUE PAGES
        // =========================================================
        for (int i = 1; i <= 3; i++) {
            logStep("[TEST 3] FTUE Page " + i + " Ad Verification...");
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            if (driver.findElements(adBannerLocator).isEmpty()) {
                reportBug("No ad banner displayed on FTUE Page " + i, "Missing_Ad_FTUE_Page_" + i);
            }
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            logStep("[ACTION] Advancing FTUE...");
            try {
                driver.findElement(AppiumBy.xpath("//*[@text='Next' or @text='Continue' or @text='Finish' or @content-desc='Next']")).click();
            } catch(Exception e) {
                // Fallback W3C tap for compose buttons
                org.openqa.selenium.Dimension size = driver.manage().window().getSize();
                performTap(size.getWidth() / 2, (int) (size.getHeight() * 0.9));
            }
            Thread.sleep(1500);
        }

        // =========================================================
        // TEST 4: SUBSCRIPTION UI (UPDATED CLOSE LOGIC)
        // =========================================================
        logStep("[ACTION] Selecting Subscription Tiers...");
        try { driver.findElement(AppiumBy.xpath("//*[contains(@text, 'Week')]")).click(); } catch(Exception ignored) {}
        try { driver.findElement(AppiumBy.xpath("//*[contains(@text, 'Month')]")).click(); } catch(Exception ignored) {}
        try { driver.findElement(AppiumBy.xpath("//*[contains(@text, 'Year') or contains(@text, 'Annual')]")).click(); } catch(Exception ignored) {}

        driver.findElement(AppiumBy.xpath("//*[@text='Continue']")).click();
        Thread.sleep(2500); // Give Google Play time to fully animate up

        logStep("[ACTION] Handling Google Play / Subscription UI Closure...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(2000); // Allow OS transition

        // THE FIX: State-Aware Second Back Press!
        // If the Sub UI is STILL blocking the screen, it means the first BACK only closed Google Play.
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue'] | //*[@text='CONTINUE'] | //*[contains(@text, 'Premium')]")).isEmpty()) {
            logStep("[ACTION] Google Play closed. Now closing Subscription Screen natively...");
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
            Thread.sleep(1500);
        }

        // Failsafe: Check if the Subscription UI is *STILL* stubbornly blocking the screen
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue'] | //*[@text='CONTINUE'] | //*[contains(@text, 'Premium')]")).isEmpty()) {
            logStep("[WARNING] Native BACK failed to close Sub UI. Engaging Ghost-X Coordinate Strikes...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();

            // Strike 1: Top Left
            performTap(80, 150);
            Thread.sleep(1000);

            // Strike 2: Top Right (if still open)
            if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue'] | //*[@text='CONTINUE']")).isEmpty()) {
                performTap(size.getWidth() - 80, 150);
                Thread.sleep(1000);
            }
        }

        // =========================================================
        // TEST 5: HOME EXIT PROMPT & WARM START
        // =========================================================
        logStep("[TEST 5] Checking for post-onboarding Ad & Waiting for Home Tab...");

        // Catch the interstitial ad that developers love to trigger right after the paywall closes!
        closeInterstitialAds();
        collapseHalfScreenAd();

        // Safely wait for the Home Tab to fully render
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify' or contains(@text, 'Identify')] | //*[@text='My Coins']")));
        } catch (Exception e) {
            reportBug("Timed out waiting for Home Tab. A rogue Ad or Paywall is likely blocking the screen.", "HomeTab_Timeout");
            // Blind tap bottom center to try and escape whatever is blocking the screen
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            performTap(size.getWidth() / 2, (int)(size.getHeight() * 0.9));
        }

        logStep("[TEST] Testing Home Tab Exit Prompt...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(2000);

        if (driver.findElements(AppiumBy.xpath("//*[contains(@text, 'sure') or contains(@text, 'Exit') or contains(@text, 'Quit')]")).isEmpty()) {
            reportBug("Exit prompt not displayed before killing the app.", "Missing_Exit_Prompt");
        }

        logStep("[ACTION] Relaunching the app for Warm Start Ad test...");
        driver.activateApp(appPackage);

        // 1. Clear the Video Ad
        closeInterstitialAds();

        // 2. NEW: Intercept the aggressive Warm-Start Subscription Paywall!
        logStep("[ACTION] Checking for Warm Start Subscription UI...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue'] | //*[contains(@text, 'Premium')]")).isEmpty()) {
            logStep("[ACTION] Paywall detected. Dismissing natively...");
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
            Thread.sleep(1500);

            // Failsafe Ghost-X strike just in case BACK doesn't work
            if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue'] | //*[contains(@text, 'Premium')]")).isEmpty()) {
                logStep("[WARNING] Native BACK failed. Engaging Ghost-X Coordinate Strikes...");
                org.openqa.selenium.Dimension size = driver.manage().window().getSize();
                performTap(80, 150); // Strike Top Left
                Thread.sleep(1000);
                if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue']")).isEmpty()) {
                    performTap(size.getWidth() - 80, 150); // Strike Top Right
                    Thread.sleep(1000);
                }
            }
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // 3. Clear the Banner Ad
        collapseHalfScreenAd();

        // =========================================================
        // TEST 6: CAMERA, PERMISSIONS, & OFFLINE PROCESSING BUG
        // =========================================================
        logStep("[TEST 6] Initiating Identification Flow...");
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan']"), "Scan Coin Button");

        logStep("[ACTION] Tapping 'Watch Ad for 1 Use'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();
        closeInterstitialAds();

        // =========================================================
        // INJECTED: THE PERMISSION SOFT-LOCK TEST
        // =========================================================
        logStep("[ACTION] Testing Camera Permission Flow...");
        By grantBtn = AppiumBy.xpath("//*[@text='Grant Permission'] | //*[@content-desc='Grant Permission']");

        // Clean XPaths using the exact resource-ids from XML
        By osDenyBtn = AppiumBy.xpath("//*[@resource-id='com.android.permissioncontroller:id/permission_deny_button' or @text='DENY']");
        By osDenyDontAskBtn = AppiumBy.xpath("//*[@resource-id='com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button' or contains(@text, 'ASK AGAIN')]");

        try {
            logStep("   -> Requesting permission (1st time)...");
            wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();

            logStep("   -> Tapping OS Deny...");
            wait.until(ExpectedConditions.elementToBeClickable(osDenyBtn)).click();
            Thread.sleep(1500);

            logStep("   -> Requesting permission (2nd time)...");
            wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();

            logStep("   -> Tapping OS Deny & Don't Ask Again...");
            // Because the locator is perfectly precise, we click it directly
            wait.until(ExpectedConditions.elementToBeClickable(osDenyDontAskBtn)).click();
            Thread.sleep(1500);

        } catch (Exception e) {
            logStep("❌ [ERROR] Permission flow interrupted! Exception: " + e.getClass().getSimpleName());
        }

        logStep("[TEST] Verifying App Soft Lock State...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(1500);

        try { driver.findElement(grantBtn).click(); } catch (Exception ignored) {}
        Thread.sleep(1500);

        if (!driver.findElements(grantBtn).isEmpty()) {
            reportBug("App soft-locked. User is unable to close the prompt and go back.", "Camera_Permission_SoftLock");
        }

        logStep("[SYSTEM] Teleporting to App Settings for Manual Permission...");
        driver.executeScript("mobile: shell", java.util.Map.of("command", "am", "args", java.util.Arrays.asList("start", "-a", "android.settings.APPLICATION_DETAILS_SETTINGS", "-d", "package:" + appPackage)));

        System.out.println("\n====================================================================");
        System.out.println("[ACTION REQUIRED] 🛑 MANUAL PERMISSION & STEP 1/2 PHOTO 🛑");
        System.out.println("1. The App Settings page should be open. Tap 'App permissions' -> 'Camera' -> 'Allow'.");
        System.out.println("2. Use the Android Recent Apps button to return to the Coin App.");
        System.out.println("3. Wait for the 'Step 1 of 2' UI to load.");
        System.out.println("4. MANUALLY take the 1st photo (Front of Coin).");
        System.out.println("5. The UI will switch to 'Step 2 of 2'.");
        System.out.println("6. DO NOT TAKE THE SECOND PHOTO. Just sit on Step 2 and press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] User returned to app. Engaging AdBuster to clear any 'Welcome Back' ad...");
        closeInterstitialAds();
        dismissPopups();

        // =========================================================
        // RESUMING: THE OFFLINE PROCESSING RACE CONDITION
        // =========================================================
        // THE FIX: We REMOVED the script clicking the Capture button here!
        // We just drop the network while on Step 2 to test background upload handling.
        logStep("[ACTION] Dropping network to test background upload handling...");

        driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("wifi", "disable")));
        driver.executeScript("mobile: shell", java.util.Map.of("command", "svc", "args", java.util.Arrays.asList("data", "disable")));

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='Retry' or contains(@text, 'Internet')]")));
        } catch (Exception ignored) {}

        logStep("[ACTION] Restoring Network...");
        setNetworkState(true);
        Thread.sleep(3000);

        logStep("[TEST] Verifying if App State was lost after offline test...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        boolean isStep1 = !driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Step 1') or contains(@text, 'STEP 1')]")).isEmpty();
        boolean isStep2 = !driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Step 2') or contains(@text, 'STEP 2')]")).isEmpty();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (isStep1 && !isStep2) {
            reportBug("App failed to cache the first image. User was dumped all the way back to Step 1 after network reconnect.", "Offline_StateLoss_CoinCapture");
        } else {
            logStep("✅ App successfully retained the 1st image and remained on Step 2!");
        }

        // =========================================================
        // NEW INJECTED TEST: EXIT & RE-ENTRY STATE CLEARING BUG
        // =========================================================
        logStep("[ACTION] Pressing Device Back to exit Camera UI and test State Clearing...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(1500);

        // Handle potential "Are you sure you want to quit?" dialog (Added 'YES' locator)
        var exitCameraPopups = driver.findElements(AppiumBy.xpath("//*[@text='Yes' or @text='Exit' or @text='Quit' or @text='Leave' or contains(@text, 'sure') or contains(@text, 'YES')]"));
        if (!exitCameraPopups.isEmpty()) {
            try {
                driver.findElement(AppiumBy.xpath("//*[@text='Yes' or @text='Exit' or @text='Leave' or @text='YES']")).click();
                Thread.sleep(1500);
            } catch (Exception ignored) {}
        }

        // Ensure we are fully back on the Home Tab
        if (driver.findElements(AppiumBy.xpath("//*[@content-desc='Scan']")).isEmpty()) {
            driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
            Thread.sleep(1500);
        }

        logStep("[ACTION] Handling Home Tab Interstitials...");
        closeInterstitialAds();

        logStep("[ACTION] Re-initiating Identification Flow to check state clearing...");
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan']"), "Scan Coin Button");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();
        closeInterstitialAds();

        logStep("[TEST] Verifying if Camera State was properly cleared on re-entry...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        boolean isStillStep2 = !driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Step 2') or contains(@text, 'STEP 2')]")).isEmpty();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        if (isStillStep2) {
            reportBug("Camera state did not clear after exiting to Home. App is stuck on Step 2 upon re-entry.", "CameraState_NotCleared_OnExit");
        } else {
            logStep("✅ Camera state successfully cleared back to Step 1.");
        }

        System.out.println("\n====================================================================");
        System.out.println("[ACTION REQUIRED] 🛑 MANUAL CAPTURE RECOVERY & AI PROCESSING 🛑");
        System.out.println("1. Please manually take whatever photo(s) are needed to process a coin.");
        System.out.println("2. Wait for the AI to process and load the Coin Description UI.");
        System.out.println("3. Once the Description UI is fully loaded, press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] Bookmarking Coin and returning to Home...");
        closeInterstitialAds();
        dismissPopups();

        try {
            driver.findElement(AppiumBy.xpath("//*[contains(@content-desc, 'Save') or contains(@content-desc, 'Add') or contains(@text, 'Collection') or contains(@text, 'Save') or contains(@resource-id, 'save') or contains(@resource-id, 'collection')]")).click();
            Thread.sleep(1000);
            logStep("✅ Successfully bookmarked the coin.");
        } catch(Exception ignored) {
            logStep("[WARNING] Could not find native Save button with expanded locators. Skipping bookmark.");
        }

        // Tap the back button to exit the description page
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back' or @content-desc='Navigate up']")).click();
        collapseHalfScreenAd();

        // =========================================================
        // TEST 7: COLLECTION & DELETION
        // =========================================================
        logStep("[TEST 7] Testing My Collection & Deletion Routing...");
        safeClick(AppiumBy.xpath("//*[@text='My Coins' or @content-desc='My Collection' or @text='My Collection']"), "My Collection Tab");

        logStep("[ACTION] Deleting the rock/coin...");
        try {
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Delete']")).click();
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Delete']"))).click();
            Thread.sleep(2500);
        } catch (Exception ignored) { logStep("[WARNING] No coin to delete or icon unreadable."); }

        if (!driver.findElements(AppiumBy.xpath("//*[@content-desc='Scan']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Collection')]")).isEmpty()) {
            reportBug("App unexpectedly navigated to Home tab after deletion instead of staying on Collection UI.", "Delete_Routing_Error");
        }

        safeClick(AppiumBy.xpath("//*[@content-desc='Home' or @text='Home']"), "Home Tab");

        // =========================================================
        // TEST 8: BACKGROUND APP STATE PRESERVATION
        // =========================================================
        logStep("[TEST 8] Testing Background Activity Preservation during Multi-Step Capture...");
        collapseHalfScreenAd();
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan']"), "Scan Coin Button");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();
        closeInterstitialAds();

        System.out.println("\n====================================================================");
        System.out.println("[ACTION REQUIRED] 🛑 PARTIAL CAPTURE 🛑");
        System.out.println("1. Please manually take exactly ONE photo.");
        System.out.println("2. The UI should display 'Step 2 of 2'.");
        System.out.println("3. DO NOT take the second photo. Press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] Backgrounding the app...");
        driver.runAppInBackground(Duration.ofSeconds(3));

        logStep("[SYSTEM] App foregrounded. Intercepting Warm Start ad...");
        closeInterstitialAds();

        logStep("[TEST] Verifying Camera State...");
        // THE FIX: Added the case-insensitive 'STEP 2' check that we used in Test 6!
        if (driver.findElements(AppiumBy.xpath("//*[contains(@text, 'Step 2') or contains(@text, 'STEP 2')]")).isEmpty()) {
            reportBug("Camera step state lost after backgrounding. App did not retain the 1st photo.", "StateLoss_Camera_Backgrounding");
        } else {
            logStep("✅ State successfully preserved.");
        }

        logStep("[ACTION] Routing back to Home...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(1500);

        // Handle potential "Are you sure you want to quit?" dialog
        exitCameraPopups = driver.findElements(AppiumBy.xpath("//*[@text='Yes' or @text='Exit' or @text='Quit' or @text='Leave' or contains(@text, 'sure') or contains(@text, 'YES')]"));
        if (!exitCameraPopups.isEmpty()) {
            try {
                driver.findElement(AppiumBy.xpath("//*[@text='Yes' or @text='Exit' or @text='Leave' or @text='YES']")).click();
                Thread.sleep(1500);
            } catch (Exception ignored) {}
        }
        // =========================================================
        // TEST 9: TEARDOWN (Handled in BaseTest)
        // =========================================================
        System.out.println("\n===============================================");
        logStep("🎉 COIN IDENTIFIER SUITE FINISHED. 🎉");
        logStep("Total Bugs Logged: " + totalBugCount);
        System.out.println("===============================================\n");

        softAssert.assertAll();
        Assert.assertEquals(totalBugCount, 0, "Suite completed, but " + totalBugCount + " bugs were found! Check the HTML report.");
    }
}