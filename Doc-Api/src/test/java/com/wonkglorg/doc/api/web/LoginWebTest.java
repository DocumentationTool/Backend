package com.wonkglorg.doc.api.web;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.wonkglorg.doc.api.service.RepoService;
import com.wonkglorg.doc.api.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Thread.sleep;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "server.port=8080")
class LoginWebTest {
    @Autowired
    protected TestRestTemplate request;

    @Autowired
    protected RepoService repoService;

    @Autowired
    protected UserService userService;

    private WebDriver driver;

    private ExtentSparkReporter extent;

    private static final Path testDir = Path.of("web-tests/reports/%s".formatted(new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())));

    @BeforeEach
    void setup() {
        // Set path to chromedriver if not in system PATH
        // System.setProperty("webdriver.chrome.driver", "path/to/chromedriver");

        driver = new ChromeDriver();
        driver.manage().window().maximize();
        extent = new ExtentSparkReporter(testDir.resolve("resource-test.html").toString());
    }


    //@Test
    void canLoginAsAdmin() throws InterruptedException, IOException {

        ExtentReports report = new ExtentReports();
        report.attachReporter(extent);
        ExtentTest test = report.createTest("Can Login As Admin");

        driver.get("http://localhost:4200");
        test.log(Status.INFO, "Navigated to login page: " + driver.getCurrentUrl());
        Assertions.assertTrue(driver.getTitle().toLowerCase().contains("login") || driver.getCurrentUrl().contains("4200"),
                "Expected to be on login page");
        test.log(Status.PASS, "Login page loaded");

        try {
            WebElement profileField = driver.findElement(By.className("profile"));
            profileField.click();
            test.log(Status.INFO, "Clicked profile icon");

            sleep(1000);

            WebElement adminItem = driver.findElement(By.xpath("//div[text()=' Admin ']"));
            Assertions.assertTrue(adminItem.isDisplayed(), "Admin option should be visible");
            test.log(Status.PASS, "'Admin' option is visible");
            adminItem.click();
            test.log(Status.INFO, "Selected 'Admin'");

            sleep(1000);

            WebElement adminField = driver.findElement(By.className("nav-admin"));
            Assertions.assertTrue(adminField.isDisplayed(), "Admin field should be visible after selecting Admin");
            test.log(Status.PASS, "Admin field is visible. Login as Admin successful");
        } catch (Exception e) {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String path = "target/screenshots/admin_fail.png";
            Files.copy(screenshot.toPath(), Paths.get(path));
            test.addScreenCaptureFromPath(path);
            test.log(Status.FAIL, "Test failed: " + e.getMessage());
            throw e;
        } finally {
            report.flush();
        }
    }

    @Test
    void resourceTest() throws InterruptedException {
        ExtentReports report = new ExtentReports();
        report.attachReporter(extent);
        ExtentTest test = report.createTest("Resource Creation, Edit, Deletion Workflow");

        try {
            driver.get("http://localhost:4200");
            test.log(Status.INFO, "Navigated to application");

            selectAdminProfile(driver, test);
            test.log(Status.PASS, "Admin profile selected");

            createResource(driver, test);
            test.log(Status.PASS, "Resource created");

            sleep(2000);

            WebElement treeNavigation = driver.findElement(By.className("nav"));
            treeNavigation.click();
            test.log(Status.INFO, "Opened tree navigation");

            sleep(2000);

            WebElement repoTreeNode = driver.findElement(By.xpath("//li[contains(@class, 'ng-star-inserted') and contains(., 'resource.md')]"));
            Assertions.assertTrue(repoTreeNode.isDisplayed(), "Resource should be visible after uploading");
            test.log(Status.PASS, "Resource is visible in repo tree");

            repoTreeNode.click();
            WebElement resource = repoTreeNode.findElements(By.className("ng-star-inserted")).get(1);

            editResource(driver, resource, test);
            test.log(Status.PASS, "Resource was edited");

            sleep(1000);
            Actions actions = new Actions(driver);
            actions.moveToElement(resource).perform();

            repoTreeNode.findElements(By.className("menu-button")).get(1).click();
            test.log(Status.INFO, "Opened context menu for resource");

            sleep(1000);

            WebElement deleteButton = driver.findElement(By.xpath("//div[contains(@class, 'popup-menu')]//li[text()='Delete resource']"));
            deleteButton.click();
            test.log(Status.INFO, "Clicked delete");

            sleep(1000);

            Alert alert = driver.switchTo().alert();
            alert.accept();
            test.log(Status.INFO, "Confirmed deletion");

            sleep(1000);
            try {
                Assertions.assertFalse(resource.isDisplayed(), "Resource should not be visible after deleting");
            } catch (Exception e) {
                //depending on version throws an exception instead of retunring false for display
                Assertions.assertTrue(true, "Resource is not visible after deletion");
            }
            test.log(Status.PASS, "Resource deleted successfully");

        } catch (Exception e) {
            captureScreenshot(test, "Failed Resource Test");
            test.log(Status.FAIL, "Failure during resource test: " + e.getMessage());
            throw e;
        } finally {
            report.flush();
        }

    }

    private void selectAdminProfile(WebDriver driver, ExtentTest test) throws InterruptedException {
        try {
            WebElement profileField = driver.findElement(By.className("profile"));
            Assertions.assertTrue(profileField.isDisplayed(), "Profile icon should be visible");
            profileField.click();
            test.log(Status.INFO, "Clicked on profile icon");

            sleep(1000);

            WebElement adminItem = driver.findElement(By.xpath("//div[text()=' Admin ']"));
            Assertions.assertTrue(adminItem.isDisplayed(), "'Admin' option should be visible in dropdown");
            adminItem.click();
            test.log(Status.INFO, "Selected 'Admin' from dropdown");

            sleep(1000);

            WebElement adminField = driver.findElement(By.className("nav-admin"));
            Assertions.assertTrue(adminField.isDisplayed(), "Admin field should be visible after selection");
            test.log(Status.PASS, "Admin view is active");
        } catch (Exception e) {
            test.log(Status.FAIL, "Failed to select admin profile: " + e.getMessage());
            throw e;
        }
    }

    private void createResource(WebDriver driver, ExtentTest test) throws InterruptedException {
        try {
            WebElement insertResource = driver.findElement(By.className("createNewFile"));
            Assertions.assertTrue(insertResource.isDisplayed(), "'Create New File' button should be visible");
            insertResource.click();
            test.log(Status.INFO, "Clicked on 'Create New File'");

            sleep(1000); // wait for modal or form

            WebElement pathField = driver.findElement(By.name("path"));
            Assertions.assertTrue(pathField.isDisplayed(), "Path input field should be visible");
            pathField.sendKeys("resource");
            test.log(Status.INFO, "Entered resource path");

            sleep(1000);

            WebElement repoIdField = driver.findElement(By.className("select"));
            repoIdField.click();
            test.log(Status.INFO, "Clicked on repo selection");

            sleep(1000);

            WebElement dropdown = driver.findElement(By.cssSelector("select.select"));
            Select select = new Select(dropdown);
            select.selectByIndex(1); // assumes first option is valid
            test.log(Status.INFO, "Selected repo from dropdown");

            captureScreenshot(test, "File Creation Values");

            WebElement uploadButton = driver.findElement(By.className("upload-btn"));
            Assertions.assertTrue(uploadButton.isDisplayed(), "Upload button should be visible");
            uploadButton.click();
            sleep(500);
            captureScreenshot(test, "File Creation");
            test.log(Status.PASS, "Resource creation submitted");
        } catch (Exception e) {
            test.log(Status.FAIL, "Failed to create resource: " + e.getMessage());
            throw e;
        }

    }

    private void editResource(WebDriver driver, WebElement resource, ExtentTest test) throws InterruptedException {
        try {
            resource.click();
            test.log(Status.INFO, "Clicked on resource to open it");

            sleep(1000);

            WebElement previewToggle = driver.findElement(By.className("preview"));
            Assertions.assertTrue(previewToggle.isDisplayed(), "Preview toggle should be visible");
            previewToggle.click();
            test.log(Status.INFO, "Switched to edit mode");
            sleep(1000);
            WebElement editorInput = driver.findElement(By.className("ace_text-input"));
            editorInput.sendKeys("Test");
            //todo:jmd add more things to it like selecting multiple fonts and styles to test them all?

            captureScreenshot(test, "Resource Edit");

            test.log(Status.INFO, "Entered content into editor");
            sleep(1000);
            previewToggle.click();
            test.log(Status.INFO, "Switched back to preview mode");

            sleep(1000);
            WebElement saveButton = driver.findElement(By.className("ri-save-line"));
            saveButton.click();
            test.log(Status.PASS, "Saved changes to resource");

        } catch (Exception e) {
            test.log(Status.FAIL, "Failed to edit resource: " + e.getMessage());
            throw e;
        }
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void captureScreenshot(ExtentTest test, String name) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path resolved = testDir.resolve(name + ".png");
            resolved.toFile().getParentFile().mkdirs();
            Files.copy(screenshot.toPath(), resolved);
            test.addScreenCaptureFromPath(resolved.toString(), name);
        } catch (IOException e) {
            test.log(Status.FAIL, "Failed to capture screenshot: " + e.getMessage());
        }
    }

}

