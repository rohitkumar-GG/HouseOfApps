package com.project.tests;

import com.project.base.BaseTest;
import com.project.pages.FTUEPage;
import com.project.pages.LanguagePage;
import com.project.pages.SplashPage;
import com.project.pages.SubscriptionPage;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class OnboardingTest extends BaseTest {

    @Test
    public void verifyOnboardingAndBugs() throws Exception {
        SoftAssert softAssert = new SoftAssert();
        SplashPage splashPage = new SplashPage(driver);
        LanguagePage langPage = new LanguagePage(driver);
        FTUEPage ftuePage = new FTUEPage(driver);
        SubscriptionPage subPage = new SubscriptionPage(driver);

        System.out.println("[TEST] Waiting for Splash Screen...");
        splashPage.waitForSplashToDisappear();
        splashPage.handleAdWallIfPresent();

        System.out.println("[TEST] Verifying Language Page...");
        langPage.clickInAppBackButton();
        langPage.selectEnglish();
        langPage.clickDone();

        System.out.println("[TEST] Progressing through FTUE...");
        ftuePage.clickNext();
        ftuePage.clickNext();
        ftuePage.clickFinish();

        System.out.println("[TEST] Verifying Subscription Page loads...");
        softAssert.assertTrue(subPage.isSubscriptionPageLoaded(), "Subscription UI did not load after FTUE.");

        System.out.println("[TEST] Checking Bug #2: Hardware Back Button...");
        ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.BACK));
        Thread.sleep(1500);

        String currentApp = ((AndroidDriver) driver).getCurrentPackage();
        softAssert.assertEquals(currentApp, "rock.identifier.diamond.gem.stone.mineral.finder.scanner",
                "CRITICAL BUG 2: Hardware back button closed the app entirely!");

        System.out.println("[TEST] Reopening the App to continue flow...");
        ((AndroidDriver) driver).activateApp("rock.identifier.diamond.gem.stone.mineral.finder.scanner");

        System.out.println("[TEST] Handling returning Ad Wall...");
        splashPage.waitForSplashToDisappear();
        splashPage.handleAdWallIfPresent();

        System.out.println("[TEST] Verifying Subscription Page loads again...");
        softAssert.assertTrue(subPage.isSubscriptionPageLoaded(), "Subscription UI did not load after reopening app.");

        System.out.println("[TEST] Clicking 'X' close button on Subscription UI...");
        subPage.clickCloseX();
        System.out.println("[TEST] Flow complete.");

        softAssert.assertAll();
    }
}