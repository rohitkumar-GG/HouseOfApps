package com.project.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class LanguagePage {
    private final AppiumDriver driver;
    private final WebDriverWait wait;

    private final By languageHeader = AppiumBy.xpath("//*[@text='Language Settings' or @text='Language']");
    private final By inAppBackButton = AppiumBy.accessibilityId("Back"); // Or your specific locator
    private final By doneButton = AppiumBy.accessibilityId("Done");

    public LanguagePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public boolean isLanguageScreenLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(languageHeader)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickInAppBackButton() {
        driver.findElement(inAppBackButton).click();
    }

    /** Dynamically scrolls to the text and clicks it */
    public void selectLanguage(String languageName) {
        try {
            // This is the native Android scroll engine. It scrolls until it finds the exact text.
            String uiAutomatorCode = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(\"" + languageName + "\"))";
            driver.findElement(AppiumBy.androidUIAutomator(uiAutomatorCode)).click();
            System.out.println("[ACTION] Selected language: " + languageName);
        } catch (Exception e) {
            System.out.println("[ERROR] Could not find language: " + languageName);
        }
    }

    public void clickDone() {
        wait.until(ExpectedConditions.elementToBeClickable(doneButton)).click();
    }
}