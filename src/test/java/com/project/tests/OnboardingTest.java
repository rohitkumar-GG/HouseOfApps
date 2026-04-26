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
        SplashPage splashPage = new SplashPage(driver);
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Identify']"))).click();

        logStep("[ACTION] Tapping 'Get Premium'...");
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

        logStep("[STEP 20] Testing Crop UI Offline Volatility...");
        setNetworkState(false);

        logStep("[ACTION] Tapping 'DONE' while offline...");
        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[contains(@text, 'Internet') or contains(@text, 'network') or contains(@text, 'connection')]")));
        } catch (Exception ignored) {}

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
        rotateBtn.click();
        Thread.sleep(500);
        rotateBtn.click();
        Thread.sleep(500);

        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        logStep("[SYSTEM] Processing image. Engaging AdBuster for post-analysis ads...");
        closeInterstitialAds();

        logStep("[TEST] Extracting analyzed stone data...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@content-desc='Save Rock']")));

        String analyzedStoneName = "Diamond";
        try {
            analyzedStoneName = driver.findElement(AppiumBy.xpath("//*[@text='Gemstone']/../preceding-sibling::android.widget.TextView[1]")).getText();
        } catch (Exception e) {
            logStep("[WARNING] Dynamic extraction failed. Defaulting to 'Diamond'.");
        }
        logStep("-> Analyzed Stone Identified As: " + analyzedStoneName);

        logStep("[ACTION] Bookmarking stone and returning to Home...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Save Rock']")).click();
        Thread.sleep(1000);
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back']")).click();

        logStep("[ACTION] Navigating to 'My Rocks'...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"))).click();

        logStep("[TEST] Verifying '1 Gem Found' and checking dynamic stone name...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='1 Gem Found']")));

        if (driver.findElements(AppiumBy.xpath("//android.widget.TextView[@text='" + analyzedStoneName + "']")).isEmpty()) {
            reportBug("Analyzed stone (" + analyzedStoneName + ") was not saved to My Collection.", "Failed_Bookmark_Write");
        } else {
            logStep("✅ " + analyzedStoneName + " successfully verified in My Collection.");
        }

        logStep("[TEST] Dropping network on Collection UI...");
        setNetworkState(false);
        setNetworkState(true);

        closeInterstitialAds();

        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='My Collection']")).isEmpty()) {
            logStep("[ACTION] App forced Home Tab. Navigating back to My Rocks...");
            driver.findElement(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']")).click();
        }

        logStep("[TEST] Verifying data persistence after reconnect...");
        if (driver.findElements(AppiumBy.xpath("//android.widget.TextView[@text='" + analyzedStoneName + "']")).isEmpty()) {
            reportBug("Collection data lost after network reconnect.", "Collection_Data_Wipe");
        }

        logStep("[ACTION] Deleting the rock...");
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Delete']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Delete']"))).click();

        Thread.sleep(2500);

        logStep("[TEST] Verifying post-deletion UI routing...");
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='My Collection']")).isEmpty()) {
            reportBug("App unexpectedly navigated to Home tab after deletion instead of staying on My Collection UI.", "Delete_Routing_Error");
        }

        System.out.println("\n===============================================");
        logStep("✅ STEP 20 COMPLETE. SUITE EXECUTION FINISHED.");
        logStep("Total Bugs Logged: " + totalBugCount);
        System.out.println("===============================================\n");

        softAssert.assertAll();
        Assert.assertEquals(totalBugCount, 0, "Suite completed, but " + totalBugCount + " bugs were found! Check the HTML report.");
    }
}