package com.project.tests;

import com.project.base.BaseTest;
import com.project.pages.FTUEPage;
import com.project.pages.LanguagePage;
import com.project.pages.SplashPage;
import com.project.pages.SubscriptionPage;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class OnboardingTest extends BaseTest {

    @Test
    public void masterOnboardingFlowTest() throws Exception {
        SoftAssert softAssert = new SoftAssert();
        SplashPage splashPage = new SplashPage(driver);
        LanguagePage langPage = new LanguagePage(driver);
        FTUEPage ftuePage = new FTUEPage(driver);
        SubscriptionPage subPage = new SubscriptionPage(driver);

        // NEW LOCATOR: Catches both traditional Ads and Jetpack Compose ViewFactoryHolder Ads
        By adBannerLocator = AppiumBy.xpath("//androidx.compose.ui.viewinterop.ViewFactoryHolder | //*[contains(@resource-id, 'ad') or contains(@resource-id, 'banner') or contains(@resource-id, 'mys-creative')]");

        System.out.println("\n========== STARTING MASTER ONBOARDING FLOW ==========");

        // STEP 1: Turn off internet BEFORE the app ever opens
        System.out.println("[STEP 1] Turning OFF Wi-Fi and Data (App is not running yet)...");
        setNetworkState(false);

        // STEP 2: Cold start offline
        System.out.println("[STEP 2] Booting app offline for the first time, expecting 'No Internet' prompt...");
        driver.activateApp(appPackage); // This officially launches the app!

        try {
            // Explicitly wait up to 15 seconds for the No Internet screen to appear
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='No Internet']")));
            System.out.println("✅ 'No Internet' screen successfully verified.");
        } catch (Exception e) {
            System.out.println("❌ BUG FOUND: 'No Internet' screen did not appear within 15 seconds!");
            takeBugScreenshot("Missing_No_Internet_Screen");
        }

        // STEP 3: Turn internet back on
        System.out.println("[STEP 3] Turning ON Wi-Fi and Data...");
        setNetworkState(true);

        System.out.println("[ACTION] Waiting 5 seconds for the app to auto-refresh and load UI...");
        try {
            // Give the OS time to establish the connection and the app time to re-render
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {}

        System.out.println("[TEST] Verifying if ad failed to load on fresh online start...");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (driver.findElements(adBannerLocator).isEmpty()) {
            System.out.println("❌ BUG FOUND: Ad failed to load upon reconnecting to internet.");
            takeBugScreenshot("Missed_Ad_On_Reconnect");
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); // Restore wait

        // STEP 5: Language UI & Scrolling
        System.out.println("[STEP 4] Waiting for Language Settings UI...");
        softAssert.assertTrue(langPage.isLanguageScreenLoaded(), "Language UI failed to load.");

        System.out.println("[STEP 5] Switching languages to test UI responsiveness...");
        langPage.selectLanguage("Hindi", true);
        langPage.selectLanguage("Korean", true);

        System.out.println("[ACTION] Scrolling to bottom...");
        langPage.selectLanguage("Spanish", true);

        System.out.println("[ACTION] Scrolling back to top...");
        // Pass FALSE so the finger drags down, bringing English back into view!
        langPage.selectLanguage("English", false);

        // STEP 6: Broken In-App Back Button Check
        System.out.println("[STEP 6] Testing In-App Back Button ('<-')...");
        langPage.clickInAppBackButton();
        langPage.clickInAppBackButton(); // Click twice as requested
        if (langPage.isLanguageScreenLoaded()) {
            System.out.println("❌ BUG FOUND: In-app back button is NON-FUNCTIONAL. No exit prompt appeared.");
            takeBugScreenshot("Broken_InApp_BackButton");
        }

        // STEP 7: Proceed to FTUE
        System.out.println("[STEP 7] Tapping Blue Tick to proceed...");
        langPage.clickDone();

        // STEP 8, 9, 10: FTUE Pages & Ad Checks
        for (int i = 1; i <= 3; i++) {
            System.out.println("[STEP " + (7+i) + "] FTUE Page " + i + " Ad Verification...");
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            if (driver.findElements(adBannerLocator).isEmpty()) {
                System.out.println("❌ BUG FOUND: No ad banner displayed on FTUE Page " + i);
                takeBugScreenshot("Missing_Ad_FTUE_Page_" + i);
            }
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            if (i < 3) {
                ftuePage.clickNext();
            } else {
                ftuePage.clickFinish(); // Step 10
            }
        }

        // STEP 11: Subscription Options
        System.out.println("[STEP 11] Subscription UI loaded. Testing Tier Selections...");
        subPage.selectAllTiers();
        subPage.clickContinue();

        // STEP 12: Google Play Overlay
        System.out.println("[STEP 12] Handling Google Play Payment UI...");
        subPage.closeGooglePlayOverlay();

        // STEP 13: Restore Purchases Bug Check
        System.out.println("[STEP 13] Verifying 'Restore Purchases' functionality...");
        subPage.clickRestorePurchases();
        try {
            // Wait 2 seconds. If we are STILL on the subscription page with no prompt, it's a bug.
            Thread.sleep(2000);
            if (!driver.findElements(AppiumBy.xpath("//*[@text='Restore purchases']")).isEmpty()) {
                System.out.println("❌ BUG FOUND: 'Restore purchases' button is completely NON-FUNCTIONAL.");
                takeBugScreenshot("Broken_Restore_Purchases");
            }
        } catch (Exception ignored) {}

        // STEP 14: Go to Home Screen
        System.out.println("[STEP 14] Closing Subscription UI...");
        subPage.closeSubscriptionUI();

        // STEP 15: Home Tab Ad Verification
        System.out.println("[STEP 15] Checking Home Screen for Ad Banner...");
        softAssert.assertTrue(!driver.findElements(AppiumBy.xpath("//*[@text='Stone Identifier']")).isEmpty(), "Home Tab did not load.");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (driver.findElements(adBannerLocator).isEmpty()) {
            System.out.println("❌ BUG FOUND: No ad banner displayed on Home Screen.");
            takeBugScreenshot("Missing_Ad_Home_Screen");
        } else {
            System.out.println("✅ Ad banner successfully displayed on Home Screen.");
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // STEP 16: Teardown
        System.out.println("[STEP 16] Test Flow Complete. Initiating Cleanup...");
        // =========================================================
        // STEP 17: DEVICE BACK BUTTON & WARM START AD TEST
        // =========================================================
        System.out.println("[STEP 17] Pressing Device BACK button from Home Tab...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(2000); // Wait for potential prompt

        if (driver.findElements(AppiumBy.xpath("//*[contains(@text, 'sure') or contains(@text, 'Exit') or contains(@text, 'Quit')]")).isEmpty()) {
            System.out.println("❌ BUG FOUND: Prompt not displayed before killing the app.");
            takeBugScreenshot("Missing_Exit_Prompt");
        }

        System.out.println("[ACTION] Relaunching the app for Warm Start full-screen ad test...");
        driver.activateApp(appPackage);

        // Wait for the app to reload and hit the AdBuster
        closeInterstitialAds();

        System.out.println("[ACTION] Handling Subscription UI after warm start...");
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue']")).isEmpty()) {
            subPage.closeSubscriptionUI();
        }

        // =========================================================
        // STEP 18: IDENTIFY, MULTI-ADS & PERMISSION SOFT LOCK
        // =========================================================
        System.out.println("[STEP 18] Tapping 'Identify' button...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Identify']"))).click();

        System.out.println("[ACTION] Tapping 'Get Premium'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Get Premium']"))).click();
        subPage.closeSubscriptionUI(); // Close the sub page again

        System.out.println("[ACTION] Tapping 'Watch Ad for 1 Use'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();

        // Engage the AdBuster to handle the multi-part rewarded video
        closeInterstitialAds();

        System.out.println("[ACTION] Testing Camera Permission Flow...");
        By grantBtn = AppiumBy.xpath("//*[@text='Grant Permission']");
        By osDenyBtn = AppiumBy.id("com.android.permissioncontroller:id/permission_deny_button");
        By osDenyDontAskBtn = AppiumBy.id("com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button");

        // Cycle 1: Grant -> Deny
        wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();
        wait.until(ExpectedConditions.elementToBeClickable(osDenyBtn)).click();

        // Cycle 2: Grant -> Deny & Don't Ask Again
        wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();
        wait.until(ExpectedConditions.elementToBeClickable(osDenyDontAskBtn)).click();

        System.out.println("[TEST] Verifying App Soft Lock State...");
        driver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Thread.sleep(1500); // Wait to see if back button works
        driver.findElement(grantBtn).click(); // Try clicking Grant again
        Thread.sleep(1500);

        // [Keep your existing Soft Lock check above this...]
        if (!driver.findElements(grantBtn).isEmpty()) {
            System.out.println("❌ BUG FOUND: App soft-locked. User is unable to close the prompt and go back.");
            takeBugScreenshot("Camera_Permission_SoftLock");
        }

        // =========================================================
        // STEP 18.5: MANUAL PERMISSION BYPASS & AD INTERCEPTION
        // =========================================================
        System.out.println("[SYSTEM] ADB 'pm grant' fails on MIUI. Teleporting to App Settings...");
        // Instantly opens the Android Settings page exactly to the Stone Identifier app
        driver.executeScript("mobile: shell", java.util.Map.of("command", "am", "args", java.util.Arrays.asList("start", "-a", "android.settings.APPLICATION_DETAILS_SETTINGS", "-d", "package:" + appPackage)));

        System.out.println("\n====================================================================");
        System.out.println("[ACTION REQUIRED] 🛑 MANUAL PERMISSION GRANT 🛑");
        System.out.println("1. Look at your phone: The App Settings page should be open.");
        System.out.println("2. Tap 'App permissions' -> 'Camera' -> 'Allow'.");
        System.out.println("3. Use the Android Recent Apps button to return to the Stone App.");
        System.out.println("4. Once you are back inside the Stone app, press [ENTER] here.");
        System.out.println("====================================================================\n");

        // Pause 1: Wait for you to grant permission and return
        new java.util.Scanner(System.in).nextLine();

        System.out.println("[ACTION] User returned to app. Engaging AdBuster to clear the 'Welcome Back' ad...");
        // The script wakes up and immediately kills the ad
        closeInterstitialAds();

        // =========================================================
        // STEP 19: HUMAN-IN-THE-LOOP (MANUAL PHOTO)
        // =========================================================
        System.out.println("\n====================================================================");
        System.out.println("[STEP 19] 🛑 MANUAL PHOTO REQUIRED 🛑");
        System.out.println("1. The Camera UI should now be active and completely ad-free.");
        System.out.println("2. Please manually take a photo of a stone (or anything).");
        System.out.println("3. Wait for the app to process and load the 'Crop Stone' UI.");
        System.out.println("4. Once you see the Rotate and DONE buttons, press [ENTER] here.");
        System.out.println("====================================================================\n");

        // Pause 2: Wait for you to take the picture
        new java.util.Scanner(System.in).nextLine();

        System.out.println("[SYSTEM] User confirmed Crop UI loaded. Test Flow Complete. Shutting down.");

        // =========================================================
        // STEP 20: CROP UI VOLATILITY & COLLECTION MANAGEMENT
        // =========================================================
        System.out.println("[STEP 20] Testing Crop UI Offline Volatility...");
        setNetworkState(false);

        System.out.println("[ACTION] Tapping 'DONE' while offline...");
        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        // Wait for the OS/App to throw a network error prompt
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[contains(@text, 'Internet') or contains(@text, 'network') or contains(@text, 'connection')]")));
        } catch (Exception ignored) {}

        setNetworkState(true);
        Thread.sleep(3000); // Give Android time to re-establish connection

        // If the 'DONE' button is gone, the photo was dumped from RAM and the camera reopened
        if (driver.findElements(AppiumBy.xpath("//*[@text='DONE']")).isEmpty()) {
            System.out.println("❌ BUG FOUND: Clicked photo failed to be retained after turning the internet back on. Camera UI re-opened.");
            takeBugScreenshot("Volatile_Memory_Loss_CropUI");
        }

        // ---------------------------------------------------------
        // HUMAN-IN-THE-LOOP (CYCLE 2)
        // ---------------------------------------------------------
        System.out.println("\n====================================================================");
        System.out.println("[ACTION REQUIRED] 🛑 SECOND MANUAL PHOTO REQUIRED 🛑");
        System.out.println("1. The photo was lost. Please manually take ANOTHER photo.");
        System.out.println("2. Wait for the 'Crop Stone' UI to load again.");
        System.out.println("3. Once you see the Rotate and DONE buttons, press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        // ---------------------------------------------------------
        // IMAGE PROCESSING & DYNAMIC EXTRACTION
        // ---------------------------------------------------------
        System.out.println("[ACTION] Tapping rotate icon twice (0.5s delay)...");
        org.openqa.selenium.WebElement rotateBtn = driver.findElement(AppiumBy.xpath("//*[@content-desc='Rotate']"));
        rotateBtn.click();
        Thread.sleep(500);
        rotateBtn.click();
        Thread.sleep(500);

        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        System.out.println("[SYSTEM] Processing image. Engaging AdBuster for post-analysis ads...");
        closeInterstitialAds(); // Blocks until the ad wall is cleared and Stone UI loads

        System.out.println("[TEST] Extracting analyzed stone data...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@content-desc='Save Rock']")));

        // Dynamically scrape the stone name (It sits right above the 'Gemstone' tag in the DOM)
        String analyzedStoneName = "Diamond"; // Fallback
        try {
            analyzedStoneName = driver.findElement(AppiumBy.xpath("//*[@text='Gemstone']/../preceding-sibling::android.widget.TextView[1]")).getText();
        } catch (Exception e) {
            System.out.println("[WARNING] Dynamic extraction failed. Defaulting to 'Diamond'.");
        }
        System.out.println("-> Analyzed Stone Identified As: " + analyzedStoneName);

        System.out.println("[ACTION] Bookmarking stone and returning to Home...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Save Rock']")).click();
        Thread.sleep(1000); // Let the database write
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back']")).click();

        // ---------------------------------------------------------
        // MY COLLECTION VERIFICATION & NETWORK RESILIENCE
        // ---------------------------------------------------------
        System.out.println("[ACTION] Navigating to 'My Rocks'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"))).click();

        System.out.println("[TEST] Verifying '1 Gem Found' and checking dynamic stone name...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='1 Gem Found']")));

        if (driver.findElements(AppiumBy.xpath("//android.widget.TextView[@text='" + analyzedStoneName + "']")).isEmpty()) {
            System.out.println("❌ BUG FOUND: Analyzed stone (" + analyzedStoneName + ") was not saved to My Collection.");
            takeBugScreenshot("Failed_Bookmark_Write");
        } else {
            System.out.println("✅ " + analyzedStoneName + " successfully verified in My Collection.");
        }

        System.out.println("[TEST] Dropping network on Collection UI...");
        setNetworkState(false);
        setNetworkState(true);

        closeInterstitialAds(); // Hunt for the "Welcome Back" ad

        // If the app dumped us back to Home after the network toggle, navigate back
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='My Collection']")).isEmpty()) {
            System.out.println("[ACTION] App forced Home Tab. Navigating back to My Rocks...");
            driver.findElement(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']")).click();
        }

        System.out.println("[TEST] Verifying data persistence after reconnect...");
        if (driver.findElements(AppiumBy.xpath("//android.widget.TextView[@text='" + analyzedStoneName + "']")).isEmpty()) {
            System.out.println("❌ BUG FOUND: Collection data lost after network reconnect.");
            takeBugScreenshot("Collection_Data_Wipe");
        }

        // ---------------------------------------------------------
        // DELETE FLOW & ROUTING BUG
        // ---------------------------------------------------------
        System.out.println("[ACTION] Deleting the rock...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Delete']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Delete']"))).click();

        Thread.sleep(2500); // Allow time for the deletion animation and potential routing

        System.out.println("[TEST] Verifying post-deletion UI routing...");
        // Check if we are incorrectly viewing the Home screen instead of My Collection
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='My Collection']")).isEmpty()) {
            System.out.println("❌ BUG FOUND: App unexpectedly navigated to Home tab after deletion instead of staying on My Collection UI.");
            takeBugScreenshot("Delete_Routing_Error");
        }

        System.out.println("\n===============================================");
        System.out.println("✅ STEP 20 COMPLETE. SUITE EXECUTION FINISHED.");
        System.out.println("===============================================");

        System.out.println("========== MASTER ONBOARDING FLOW FINISHED ==========\n");
        softAssert.assertAll();
    }
}