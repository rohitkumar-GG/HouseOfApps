package com.project.base;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.net.URI;
import java.time.Duration;

public class BaseTest {
    protected AndroidDriver driver;

    @BeforeMethod
    public void setupDriver() throws Exception {
        // Reads the selection from the MenuRunner (Defaults to Stone if run via IDE)
        String appTarget = System.getProperty("targetApp", "Stone");

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("83fc08ef");
        options.setNoReset(true);
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);

        if (appTarget.equalsIgnoreCase("Stone")) {
            options.setAppPackage("rock.identifier.diamond.gem.stone.mineral.finder.scanner");
            options.setAppActivity("rock.identifier.diamond.gem.stone.mineral.finder.scanner.MainActivity");
        }
        // Add Coin capabilities here later!

        driver = new AndroidDriver(new URI("http://127.0.0.1:4723").toURL(), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}