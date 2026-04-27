package com.project.tests;

import com.project.base.BaseTest;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.Duration;

public class IdentifyFeatureTest extends BaseTest {

    @Test
    public void isolatedIdentifyTest() throws Exception {
        SoftAssert softAssert = new SoftAssert();
        logStep("\n========== STARTING ISOLATED IDENTIFY FEATURE TEST ==========");

        // App is already launched and on Home tab (as per CLI Pre-Condition)
        driver.activateApp(appPackage);

        collapseHalfScreenAd();
        closeInterstitialAds();

        logStep("[ACTION] Tapping 'Identify' via Safe-Zone Bottom Nav...");
        safeClick(AppiumBy.xpath("//*[@content-desc='Scan'] | //*[@text='Identify' or @text='Identify a Rock']"), "Scan/Identify Button");

        logStep("[ACTION] Tapping 'Watch Ad for 1 Use'...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Watch Ad for 1 Use']"))).click();

        logStep("[ACTION] Watching rewarded ad(s)...");
        closeInterstitialAds();

        System.out.println("\n====================================================================");
        logStep("[ACTION REQUIRED] 🛑 MANUAL PHOTO REQUIRED 🛑");
        logStep("1. The Ad is clear. Please manually take a photo.");
        logStep("2. Once you see the Crop Stone DONE button, press [ENTER] here.");
        System.out.println("====================================================================\n");
        new java.util.Scanner(System.in).nextLine();

        logStep("[ACTION] Tapping 'DONE' and testing processing state...");
        wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='DONE']"))).click();
        Thread.sleep(1500);

        logStep("[SYSTEM] Processing image. Engaging AdBuster for post-analysis ads...");
        closeInterstitialAds();
        dismissPopups();

        logStep("[TEST] Extracting analyzed stone data (Waiting up to 30s for AI Processing)...");
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@content-desc='Save Rock'] | //*[@text='Not a Stone']")));

        if (!driver.findElements(AppiumBy.xpath("//*[@text='Not a Stone']")).isEmpty()) {
            logStep("-> AI Result: NOT A STONE");
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
        } else {
            String lastStoneAnalyzed = "Unknown";
            try {
                lastStoneAnalyzed = driver.findElement(AppiumBy.xpath("//*[@text='Gemstone']/../preceding-sibling::android.widget.TextView[1]")).getText();
            } catch (Exception ignored) {}
            logStep("-> Analysis Restored Stone As: " + lastStoneAnalyzed);
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Back'] | //*[@content-desc='Navigate up']")).click();
        }

        System.out.println("\n===============================================");
        logStep("✅ ISOLATED IDENTIFY TEST COMPLETE.");
        logStep("Total Bugs Logged: " + totalBugCount);
        System.out.println("===============================================\n");

        softAssert.assertAll();
        Assert.assertEquals(totalBugCount, 0, "Test completed with bugs.");
    }
}