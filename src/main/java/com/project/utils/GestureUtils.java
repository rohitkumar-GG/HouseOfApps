package com.project.utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import java.time.Duration;
import java.util.Arrays;

public class GestureUtils {

    public static void performDoubleTap(AndroidDriver driver, WebElement element) {
        try {
            Dimension size = element.getSize();
            org.openqa.selenium.Point location = element.getLocation();
            int centerX = location.getX() + (size.getWidth() / 2);
            int centerY = location.getY() + (size.getHeight() / 2);
            performDoubleTapCoords(driver, centerX, centerY);
        } catch (Exception e) { ReportManager.logStep("[ERROR] Element double-tap failed."); }
    }

    public static void performDoubleTapCoords(AndroidDriver driver, int x, int y) {
        ReportManager.logStep("[ACTION] Performing coordinate double-tap at X:" + x + " Y:" + y);
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tapSequence = new Sequence(finger, 1);
            tapSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            tapSequence.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(50)));
            tapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Arrays.asList(tapSequence));
            Thread.sleep(1000);
        } catch (Exception e) { ReportManager.logStep("[ERROR] Coordinate tap failed."); }
    }

    public static void performTap(AndroidDriver driver, int x, int y) {
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Arrays.asList(tap));
        } catch (Exception ignored) {}
    }

    public static void scrollVertical(AndroidDriver driver, boolean scrollViewDown) {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * (scrollViewDown ? 0.7 : 0.3));
            int endY = (int) (size.getHeight() * (scrollViewDown ? 0.3 : 0.7));
            performSwipe(driver, startX, startY, startX, endY);
            Thread.sleep(1000);
        } catch (Exception ignored) {}
    }

    public static void scrollHorizontal(AndroidDriver driver, int yCoordinate, boolean swipeViewRight) {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = (int) (size.getWidth() * (swipeViewRight ? 0.8 : 0.2));
            int endX = (int) (size.getWidth() * (swipeViewRight ? 0.2 : 0.8));
            performSwipe(driver, startX, yCoordinate, endX, yCoordinate);
            Thread.sleep(1000);
        } catch (Exception ignored) {}
    }

    private static void performSwipe(AndroidDriver driver, int startX, int startY, int endX, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Arrays.asList(swipe));
    }
}