package com.project.tests.stone;

import com.project.base.BaseTest;
import com.project.pages.FTUEPage;
import com.project.pages.LanguagePage;
import com.project.pages.SubscriptionPage;
import com.project.utils.ReportManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.Duration;

public class StoneOnboardingTest extends BaseTest {

    SoftAssert softAssert = new SoftAssert();
    By adBannerLocator = AppiumBy.xpath("//androidx.compose.ui.viewinterop.ViewFactoryHolder | //*[contains(@resource-id, 'ad') or contains(@resource-id, 'banner') or contains(@resource-id, 'mys-creative')]");

    @Test(priority = 1)
    public void test01_OfflineBootAndReconnect() {
        beginTestStep("Test 1: Offline Boot", "Verifying app behavior without internet connection.");
        setNetworkState(false);
        driver.activateApp(appPackage);

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='No Internet']")));
            logStep("✅ 'No Internet' screen successfully verified.");
        } catch (Exception e) {
            reportBug("'No Internet' screen did not appear within 15 seconds!", "Missing_No_Internet_Screen");
        }

        setNetworkState(true);
        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (driver.findElements(adBannerLocator).isEmpty()) {
            reportBug("Ad failed to load upon reconnecting to internet.", "Missed_Ad_On_Reconnect");
        }
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        endTestStep();
    }

    @Test(priority = 2)
    public void test02_LanguageAndFTUE() {
        beginTestStep("Test 2: FTUE Navigation", "Testing language selection and FTUE carousel.");
        LanguagePage langPage = new LanguagePage(driver);
        FTUEPage ftuePage = new FTUEPage(driver);

        softAssert.assertTrue(langPage.isLanguageScreenLoaded(), "Language UI failed to load.");
        langPage.selectLanguage("Hindi", true);
        langPage.selectLanguage("Korean", true);
        langPage.selectLanguage("Spanish", true);
        langPage.selectLanguage("English", false);

        langPage.clickInAppBackButton();
        langPage.clickInAppBackButton();
        if (langPage.isLanguageScreenLoaded()) {
            reportBug("In-app back button is NON-FUNCTIONAL. No exit prompt appeared.", "Broken_InApp_BackButton");
        }

        langPage.clickDone();

        for (int i = 1; i <= 3; i++) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            if (driver.findElements(adBannerLocator).isEmpty()) reportBug("No ad banner displayed on FTUE Page " + i, "Missing_Ad_FTUE_Page_" + i);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            if (i < 3) ftuePage.clickNext();
            else ftuePage.clickFinish();
        }
        endTestStep();
    }

    @Test(priority = 3)
    public void test03_SubscriptionUI() throws InterruptedException {
        beginTestStep("Test 3: Subscriptions", "Validating paywall interaction and Home Screen transition.");
        SubscriptionPage subPage = new SubscriptionPage(driver);

        subPage.selectAllTiers();
        subPage.clickContinue();
        subPage.closeGooglePlayOverlay();
        dismissCashPaymentPopup();
        subPage.clickRestorePurchases();

        try {
            Thread.sleep(2000);
            if (!driver.findElements(AppiumBy.xpath("//*[@text='Restore purchases']")).isEmpty()) {
                reportBug("'Restore purchases' button is completely NON-FUNCTIONAL.", "Broken_Restore_Purchases");
            }
        } catch (Exception ignored) {}

        subPage.closeSubscriptionUI();

        softAssert.assertTrue(!driver.findElements(AppiumBy.xpath("//*[@text='Stone Identifier']")).isEmpty(), "Home Tab did not load.");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        if (driver.findElements(adBannerLocator).isEmpty()) reportBug("No ad banner displayed on Home Screen.", "Missing_Ad_Home_Screen");
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        endTestStep();
    }

    @Test(priority = 4)
    public void test04_WarmStartAndCameraPermissions() throws InterruptedException {
        beginTestStep("Test 4: Permissions & Warm Start", "Testing OS level app killing and permission soft locks.");
        driver.pressKey(new KeyEvent(AndroidKey.BACK));
        Thread.sleep(2000);

        if (driver.findElements(AppiumBy.xpath("//*[contains(@text, 'sure') or contains(@text, 'Exit') or contains(@text, 'Quit')]")).isEmpty()) {
            reportBug("Prompt not displayed before killing the app.", "Missing_Exit_Prompt");
        }

        driver.activateApp(appPackage);
        try {
            WebDriverWait splashWait = new WebDriverWait(driver, Duration.ofSeconds(15));
            splashWait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath(
                    "//*[@text='Continue'] | //*[@content-desc='Scan'] | //*[@text='Identify a Rock'] | //*[@resource-id='video_container']"
            )));
        } catch (Exception ignored) {}

        Thread.sleep(3000);
        closeInterstitialAds();

        if (!driver.findElements(AppiumBy.xpath("//*[@text='Continue']")).isEmpty()) {
            new SubscriptionPage(driver).closeSubscriptionUI();
        }

        safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify' or @text='Identify a Rock']"), "Scan/Identify Button");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Get Premium']"))).click();
        new SubscriptionPage(driver).closeSubscriptionUI();
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();
        closeInterstitialAds();

        By grantBtn = AppiumBy.xpath("//*[@text='Grant Permission']");
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            shortWait.until(ExpectedConditions.visibilityOfElementLocated(grantBtn));

            wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.id("com.android.permissioncontroller:id/permission_deny_button"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(grantBtn)).click();
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.id("com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button"))).click();

            driver.navigate().back();
            Thread.sleep(1500);
            driver.findElement(grantBtn).click();
            Thread.sleep(1500);

            if (!driver.findElements(grantBtn).isEmpty()) {
                reportBug("App soft-locked. User is unable to close the prompt and go back.", "Camera_Permission_SoftLock");
            }

            driver.executeScript("mobile: shell", java.util.Map.of("command", "am", "args", java.util.Arrays.asList("start", "-a", "android.settings.APPLICATION_DETAILS_SETTINGS", "-d", "package:" + appPackage)));

            System.out.println("\n[ACTION REQUIRED] 🛑 MANUAL PERMISSION GRANT 🛑 \n1. Allow Camera in App Settings.\n2. Return to the App.\n3. Press [ENTER] here.");
            new java.util.Scanner(System.in).nextLine();
            closeInterstitialAds();

        } catch (Exception e) {
            logStep("Permissions already granted. Skipping.");
        }

        System.out.println("\n[ACTION REQUIRED] 🛑 MANUAL PHOTO REQUIRED 🛑 \nTake a photo and wait for the CROP UI. Press [ENTER] when ready.");
        new java.util.Scanner(System.in).nextLine();
        endTestStep();
    }

    @Test(priority = 5)
    public void test05_CropVolatilityAndBookmark() throws InterruptedException {
        beginTestStep("Test 5: Crop UI Volatility", "Testing offline volatility of the camera processing screen.");
        setNetworkState(false);
        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        try { wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[contains(@text, 'Internet')]"))); } catch (Exception ignored) {}
        setNetworkState(true);
        Thread.sleep(3000);

        if (driver.findElements(AppiumBy.xpath("//*[@text='DONE']")).isEmpty()) {
            reportBug("Clicked photo failed to be retained after turning the internet back on. Camera UI re-opened.", "Volatile_Memory_Loss_CropUI");
            System.out.println("\n[ACTION REQUIRED] 🛑 SECOND PHOTO REQUIRED 🛑 \nTake another photo, wait for CROP UI, Press [ENTER].");
            new java.util.Scanner(System.in).nextLine();
        }

        org.openqa.selenium.WebElement rotateBtn = driver.findElement(AppiumBy.xpath("//*[@content-desc='Rotate']"));
        rotateBtn.click(); Thread.sleep(500); rotateBtn.click(); Thread.sleep(500);
        driver.findElement(AppiumBy.xpath("//*[@text='DONE']")).click();

        try { new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.invisibilityOfElementLocated(AppiumBy.xpath("//*[@text='DONE']"))); } catch (Exception ignored) {}
        closeInterstitialAds();

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@content-desc='Save Rock'] | //*[@text='Save Rock']")));
        collapseHalfScreenAd();

        driver.findElement(AppiumBy.xpath("//*[@content-desc='Save Rock']")).click();
        Thread.sleep(1000);
        driver.findElement(AppiumBy.xpath("//*[@content-desc='Back']")).click();
        collapseHalfScreenAd();
        safeClick(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"), "My Rocks Tab");
        endTestStep();
    }

    // You can now seamlessly paste your exact logic for Step 21 into test06_DoubleCaptureRaceCondition()
    // Step 22 into test07_InterruptionStressTest()
    // Step 23 into test08_SearchAndNavigation()
    // Step 24 into test09_NetworkDropBenchmark()
    // Step 25 into test10_BackgroundingBenchmark()

    @Test(priority = 11, alwaysRun = true)
    public void test11_FinalSuiteTeardown() {
        beginTestStep("Test 11: Teardown", "Clearing app data and evaluating suite success.");
        clearAppData();
        endTestStep();

        System.out.println("\n===============================================");
        logStep("🎉 SUITE COMPLETE. THE FULL STONE IDENTIFIER SUITE IS FINISHED. 🎉");
        logStep("Total Bugs Logged: " + ReportManager.totalBugCount);
        System.out.println("===============================================\n");

        softAssert.assertAll();
        Assert.assertEquals(ReportManager.totalBugCount, 0, "Suite completed, but bugs were found! Check the HTML report.");
    }
}