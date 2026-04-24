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

    private final By headerText = AppiumBy.xpath("//android.widget.TextView[contains(@text, 'Identify rocks')]");
    private final By closeXButton = AppiumBy.accessibilityId("Close");

    public SubscriptionPage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public boolean isSubscriptionPageLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(headerText)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickCloseX() {
        wait.until(ExpectedConditions.elementToBeClickable(closeXButton)).click();
    }
}