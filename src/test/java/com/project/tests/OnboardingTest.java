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

        // Generic Ad Locator (Adjust if specific ID is known)
        By adBannerLocator = AppiumBy.xpath("//android.view.View[contains(@resource-id, 'ad') or contains(@resource-id, 'mys-creative')]");

        System.out.println("\n========== STARTING MASTER ONBOARDING FLOW ==========");

        // STEP 1 & 2: Turn off internet and verify No Internet Screen
        System.out.println("[STEP 1] Turning OFF Wi-Fi and Data...");
        setNetworkState(false);

        System.out.println("[STEP 2] Launching app, expecting 'No Internet' prompt...");
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

        // STEP 4 & 5: Language UI & Scrolling
        System.out.println("[STEP 4] Waiting for Language Settings UI...");
        softAssert.assertTrue(langPage.isLanguageScreenLoaded(), "Language UI failed to load.");

        System.out.println("[STEP 5] Switching languages to test UI responsiveness...");
        langPage.selectLanguage("Hindi");
        langPage.selectLanguage("Korean");
        System.out.println("[ACTION] Scrolling to bottom...");
        langPage.selectLanguage("Spanish");
        System.out.println("[ACTION] Scrolling back to top...");
        langPage.selectLanguage("English");

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

        System.out.println("========== MASTER ONBOARDING FLOW FINISHED ==========\n");
        softAssert.assertAll();
    }
}