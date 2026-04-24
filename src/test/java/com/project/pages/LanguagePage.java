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

    private final By englishOption = AppiumBy.xpath("//android.widget.TextView[@text='English']");
    private final By backButton = AppiumBy.accessibilityId("Back");
    private final By languageHeader = AppiumBy.xpath("//android.widget.TextView[@text='Language Settings']");

    // NEW: The checkmark at the top right
    private final By doneButton = AppiumBy.accessibilityId("Done");

    public LanguagePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public boolean isLanguageScreenLoaded() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(languageHeader)).isDisplayed();
    }

    public void clickInAppBackButton() {
        driver.findElement(backButton).click();
    }

    public void selectEnglish() {
        wait.until(ExpectedConditions.elementToBeClickable(englishOption)).click();
    }

    // NEW: Method to click the checkmark
    public void clickDone() {
        wait.until(ExpectedConditions.elementToBeClickable(doneButton)).click();
    }
}