package com.project.tests;

import com.project.base.BaseTest;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.Duration;

public class CollectionTest extends BaseTest {

    @Test
    public void isolatedCollectionTest() throws Exception {
        SoftAssert softAssert = new SoftAssert();
        logStep("\n========== STARTING ISOLATED COLLECTION & DELETION TEST ==========");

        driver.activateApp(appPackage);
        collapseHalfScreenAd();
        closeInterstitialAds();

        logStep("[ACTION] Navigating to 'My Rocks'...");
        safeClick(AppiumBy.xpath("//*[@text='My Rocks' or @content-desc='My Rocks']"), "My Rocks Tab");

        logStep("[TEST] Verifying '1 Gem Found' or 'My Collection' header...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(AppiumBy.xpath("//*[@text='1 Gem Found' or @text='My Collection']")));

        logStep("[ACTION] Deleting the first rock in the collection...");
        try {
            driver.findElement(AppiumBy.xpath("//*[@content-desc='Delete']")).click();
            wait.until(ExpectedConditions.elementToBeClickable(AppiumBy.xpath("//*[@text='Delete']"))).click();
            Thread.sleep(2500); // Allow time for animation
        } catch (Exception e) {
            reportBug("Failed to find or click the Delete icon. Is the collection empty?", "Delete_Action_Failed");
        }

        logStep("[TEST] Verifying post-deletion UI routing...");
        if (!driver.findElements(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']")).isEmpty() && driver.findElements(AppiumBy.xpath("//*[@text='My Collection']")).isEmpty()) {
            reportBug("App unexpectedly navigated to Home tab after deletion instead of staying on My Collection UI.", "Delete_Routing_Error");
        } else {
            logStep("✅ Stayed on Collection UI after deletion.");
        }

        logStep("[ACTION] Returning to Home Tab...");
        safeClick(AppiumBy.xpath("//*[@text='Home' or @content-desc='Home']"), "Home Tab");

        System.out.println("\n===============================================");
        logStep("✅ ISOLATED COLLECTION TEST COMPLETE.");
        logStep("Total Bugs Logged: " + totalBugCount);
        System.out.println("===============================================\n");

        softAssert.assertAll();
        Assert.assertEquals(totalBugCount, 0, "Test completed with bugs.");
    }
}