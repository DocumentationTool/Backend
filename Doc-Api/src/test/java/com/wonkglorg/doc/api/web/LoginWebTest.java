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
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, properties = "server.port=8080")
class LoginWebTest{
	
	//todo:jmd random ports not working duo to how pipelines in jenkins would work where the port 8080 will be in use
	
	@Autowired
	protected TestRestTemplate request;
	
	@Autowired
	protected RepoService repoService;
	
	@Autowired
	protected UserService userService;
	
	private WebDriver driver;
	
	private ExtentSparkReporter extent;
	
	@BeforeEach
	public void setup() {
		// Set path to chromedriver if not in system PATH
		// System.setProperty("webdriver.chrome.driver", "path/to/chromedriver");
		
		driver = new ChromeDriver();
		driver.manage().window().maximize();
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		extent = new ExtentSparkReporter("target/extent-reports/login-web-test-%s.html".formatted(timestamp));
	}
	
	@Test
	void canLoginAsAdmin() throws InterruptedException, IOException {
		
		ExtentReports report = new ExtentReports();
		report.attachReporter(extent);
		ExtentTest test = report.createTest("Can Login As Admin");
		
		driver.get("http://localhost:4200");
		test.log(Status.INFO, "Navigated to login page");
		
		try{
			WebElement profileField = driver.findElement(By.className("profile"));
			profileField.click();
			test.log(Status.INFO, "Clicked profile icon");
			
			Thread.sleep(1000);
			
			WebElement adminItem = driver.findElement(By.xpath("//div[text()=' Admin ']"));
			adminItem.click();
			test.log(Status.INFO, "Selected 'Admin' option");
			
			Thread.sleep(1000);
			
			WebElement adminField = driver.findElement(By.className("nav-admin"));
			Assertions.assertTrue(adminField.isDisplayed(), "Admin field should be visible after selecting Admin");
			test.log(Status.PASS, "Admin field is visible");
		} catch(Exception e){
			File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			String path = "target/screenshots/admin_fail.png";
			Files.copy(screenshot.toPath(), Paths.get(path));
			test.log(Status.FAIL, "Test failed: " + e.getMessage());
			throw e;
		} finally{
			report.flush();
		}
	}
	
	@Test
	void resourceTest() throws InterruptedException {
		
		driver.get("http://localhost:4200");
		
		WebElement profileField = driver.findElement(By.className("profile"));
		profileField.click();
		
		Thread.sleep(1000);
		
		WebElement adminItem = driver.findElement(By.xpath("//div[text()=' Admin ']"));
		adminItem.click();
		
		Thread.sleep(1000);
		
		WebElement adminField = driver.findElement(By.className("nav-admin"));
		Assertions.assertTrue(adminField.isDisplayed(), "Admin field should be visible after selecting Admin");
		
		WebElement insertResource = driver.findElement(By.className("createNewFile"));
		Assertions.assertTrue(insertResource.isDisplayed(), "Insert Resource should be visible after selecting Admin");
		
		insertResource.click();
		
		Thread.sleep(1000);
		
		WebElement pathField = driver.findElement(By.name("path"));
		pathField.sendKeys("resource");
		
		Thread.sleep(1000);
		
		WebElement repoIdField = driver.findElement(By.className("select"));
		repoIdField.click();
		
		Thread.sleep(1000);
		
		WebElement dropdown = driver.findElement(By.cssSelector("select.select"));
		
		// Wrap it in a Select object
		Select select = new Select(dropdown);
		
		// Select the first non-disabled option (index 1, since index 0 is disabled/empty)
		select.selectByIndex(1); // this would select "testRepo1"
		
		WebElement uploadButton = driver.findElement(By.className("upload-btn"));
		uploadButton.click();
		
		Thread.sleep(2000);
		
		WebElement treeNavigation = driver.findElement(By.className("nav"));
		treeNavigation.click();
		Thread.sleep(2000);
		WebElement resource = driver.findElement(By.xpath("//li[contains(@class, 'ng-star-inserted') and contains(., 'resource.md')]"));
		Assertions.assertTrue(resource.isDisplayed(), "Resource should be visible after uploading");
		
		resource.click();
		
		
		
		Actions actions = new Actions(driver);
		actions.moveToElement(resource).perform();
		
		Thread.sleep(1000);
		
		WebElement optionsButton = driver.findElement(By.className("popup-menu"));
		optionsButton.click();
		Thread.sleep(1000);
		
	}
	
	@AfterEach
	public void teardown() {
		if(driver != null){
			driver.quit();
		}
	}
}

