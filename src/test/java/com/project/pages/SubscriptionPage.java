package com.project.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class SubscriptionPage {
    private final AppiumDriver driver;
    private final WebDriverWait wait;

    // Based on "Subscription ui 1.xml"
    private final By weeklyTier = AppiumBy.xpath("//*[@text='Weekly']");
    private final By monthlyTier = AppiumBy.xpath("//*[@text='Monthly']");
    private final By annualTier = AppiumBy.xpath("//*[@text='Annual']");
    private final By continueButton = AppiumBy.xpath("//*[@text='Continue']");
    private final By restoreButton = AppiumBy.xpath("//*[@text='Restore purchases']");

    // Close button for Subscription UI
    // FIX: Using structural XPath because the dev left this button completely unlabeled
    private final By closeButton = AppiumBy.xpath("//android.widget.ScrollView/android.view.View[1]");

    // Google Play UI Close Button (Based on "Google Play payment UI.xml")
    private final By googlePlayCloseBtn = AppiumBy.accessibilityId("Close");

    public SubscriptionPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void selectAllTiers() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(weeklyTier)).click();
        Thread.sleep(500); // Brief pause to simulate human reading
        driver.findElement(monthlyTier).click();
        Thread.sleep(500);
        driver.findElement(annualTier).click();
    }

    public void clickContinue() {
        driver.findElement(continueButton).click();
    }

    public void closeGooglePlayOverlay() {
        wait.until(ExpectedConditions.elementToBeClickable(googlePlayCloseBtn)).click();
    }

    public void clickRestorePurchases() {
        driver.findElement(restoreButton).click();
    }

    public void closeSubscriptionUI() {
        wait.until(ExpectedConditions.elementToBeClickable(closeButton)).click();
    }
}