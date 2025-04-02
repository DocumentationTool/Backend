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

            WebElement repoTreeNode = driver.findElement(By.xpath("//li[contains(@class, 'ng-star-inserted') and contains(., 'script.md')]"));
            Assertions.assertTrue(repoTreeNode.isDisplayed(), "Resource should be visible after uploading");
            test.log(Status.PASS, "Resource is visible in repo tree");

            repoTreeNode.click();
            WebElement resource = repoTreeNode.findElements(By.className("ng-star-inserted")).get(1);

            editResource(driver, resource, test);
            test.log(Status.PASS, "Resource was edited");

            sleep(3000);
            Actions actions = new Actions(driver);
            actions.moveToElement(resource).perform();

            repoTreeNode.findElements(By.className("menu-button")).get(1).click();
            test.log(Status.INFO, "Opened context menu for resource");

            sleep(5000);

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
            pathField.sendKeys("script");
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
			Actions actions = new Actions(driver);
			actions.doubleClick(editorInput).click(editorInput).perform();
			editorInput.sendKeys("# Bee Movie Script");
            editorInput.sendKeys(INPUT_TEXT);

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
    
    
    private static final String INPUT_TEXT = """
					\n
					According to all known laws
					of aviation,
					there is no way a bee
					should be able to fly.
					Its wings are too small to get
					its fat little body off the ground.
					The bee, of course, flies anyway
					because bees don't care
					what humans think is impossible.
					Yellow, black. Yellow, black.
					Yellow, black. Yellow, black.
					Ooh, black and yellow!
					Let's shake it up a little.
					Barry! Breakfast is ready!
					Ooming!
					Hang on a second.
					Hello?
					- Barry?
					- Adam?
					- Oan you believe this is happening?
					- I can't. I'll pick you up.
					Looking sharp.
					Use the stairs. Your father
					paid good money for those.
					Sorry. I'm excited.
					Here's the graduate.
					We're very proud of you, son.
					A perfect report card, all B's.
					Very proud.
					Ma! I got a thing going here.
					- You got lint on your fuzz.
					- Ow! That's me!
					- Wave to us! We'll be in row 118,000.
					- Bye!
					Barry, I told you,
					stop flying in the house!
					- Hey, Adam.
					- Hey, Barry.
					- Is that fuzz gel?
					- A little. Special day, graduation.
					Never thought I'd make it.
					Three days grade school,
					three days high school.
					Those were awkward.
					Three days college. I'm glad I took
					a day and hitchhiked around the hive.
					You did come back different.
					- Hi, Barry.
					- Artie, growing a mustache? Looks good.
					- Hear about Frankie?
					- Yeah.
					- You going to the funeral?
					- No, I'm not going.
					Everybody knows,
					sting someone, you die.
					Don't waste it on a squirrel.
					Such a hothead.
					I guess he could have
					just gotten out of the way.
					I love this incorporating
					an amusement park into our day.
					That's why we don't need vacations.
					Boy, quite a bit of pomp...
					under the circumstances.
					- Well, Adam, today we are men.
					- We are!
					- Bee-men.
					- Amen!
					Hallelujah!
					Students, faculty, distinguished bees,
					please welcome Dean Buzzwell.
					Welcome, New Hive Oity
					graduating class of...
					...9:15.
					That concludes our ceremonies.
					And begins your career
					at Honex Industries!
					Will we pick ourjob today?
					I heard it's just orientation.
					Heads up! Here we go.
					Keep your hands and antennas
					inside the tram at all times.
					- Wonder what it'll be like?
					- A little scary.
					Welcome to Honex,
					a division of Honesco
					and a part of the Hexagon Group.
					This is it!
					Wow.
					Wow.
					We know that you, as a bee,
					have worked your whole life
					to get to the point where you
					can work for your whole life.
					Honey begins when our valiant Pollen
					Jocks bring the nectar to the hive.
					Our top-secret formula
					is automatically color-corrected,
					scent-adjusted and bubble-contoured
					into this soothing sweet syrup
					with its distinctive
					golden glow you know as...
					Honey!
					- That girl was hot.
					- She's my cousin!
					- She is?
					- Yes, we're all cousins.
					- Right. You're right.
					- At Honex, we constantly strive
					to improve every aspect
					of bee existence.
					These bees are stress-testing
					a new helmet technology.
					- What do you think he makes?
					- Not enough.
					Here we have our latest advancement,
					the Krelman.
					- What does that do?
					- Oatches that little strand of honey
					that hangs after you pour it.
					Saves us millions.
					Oan anyone work on the Krelman?
					Of course. Most bee jobs are
					small ones. But bees know
					that every small job,
					if it's done well, means a lot.
					But choose carefully
					because you'll stay in the job
					you pick for the rest of your life.
					The same job the rest of your life?
					I didn't know that.
					What's the difference?
					You'll be happy to know that bees,
					as a species, haven't had one day off
					in 27 million years.
					So you'll just work us to death?
					We'll sure try.
					Wow! That blew my mind!
					"What's the difference?"
					How can you say that?
					One job forever?
					That's an insane choice to have to make.
					I'm relieved. Now we only have
					to make one decision in life.
					But, Adam, how could they
					never have told us that?
					Why would you question anything?
					We're bees.
					We're the most perfectly
					functioning society on Earth.
					You ever think maybe things
					work a little too well here?
					Like what? Give me one example.
					I don't know. But you know
					what I'm talking about.
					Please clear the gate.
					Royal Nectar Force on approach.
					Wait a second. Oheck it out.
					- Hey, those are Pollen Jocks!
					- Wow.
					I've never seen them this close.
					They know what it's like
					outside the hive.
					Yeah, but some don't come back.
					- Hey, Jocks!
					- Hi, Jocks!
					You guys did great!
					You're monsters!
					You're sky freaks! I love it! I love it!
					- I wonder where they were.
					- I don't know.
					Their day's not planned.
					Outside the hive, flying who knows
					where, doing who knows what.
					You can'tjust decide to be a Pollen
					Jock. You have to be bred for that.
					Right.
					Look. That's more pollen
					than you and I will see in a lifetime.
					It's just a status symbol.
					Bees make too much of it.
					Perhaps. Unless you're wearing it
					and the ladies see you wearing it.
					Those ladies?
					Aren't they our cousins too?
					Distant. Distant.
					Look at these two.
					- Oouple of Hive Harrys.
					- Let's have fun with them.
					It must be dangerous
					being a Pollen Jock.
					Yeah. Once a bear pinned me
					against a mushroom!
					He had a paw on my throat,
					and with the other, he was slapping me!
					- Oh, my!
					- I never thought I'd knock him out.
					What were you doing during this?
					Trying to alert the authorities.
					I can autograph that.
					A little gusty out there today,
					wasn't it, comrades?
					Yeah. Gusty.
					We're hitting a sunflower patch
					six miles from here tomorrow.
					- Six miles, huh?
					- Barry!
					A puddle jump for us,
					but maybe you're not up for it.
					- Maybe I am.
					- You are not!
					We're going 0900 at J-Gate.
					What do you think, buzzy-boy?
					Are you bee enough?
					I might be. It all depends
					on what 0900 means.
					Hey, Honex!
					Dad, you surprised me.
					You decide what you're interested in?
					- Well, there's a lot of choices.
					- But you only get one.
					Do you ever get bored
					doing the same job every day?
					Son, let me tell you about stirring.
					You grab that stick, and you just
					move it around, and you stir it around.
					You get yourself into a rhythm.
					It's a beautiful thing.
					You know, Dad,
					the more I think about it,
					maybe the honey field
					just isn't right for me.
					You were thinking of what,
					making balloon animals?
					That's a bad job
					for a guy with a stinger.
					Janet, your son's not sure
					he wants to go into honey!
					- Barry, you are so funny sometimes.
					- I'm not trying to be funny.
					You're not funny! You're going
					into honey. Our son, the stirrer!
					- You're gonna be a stirrer?
					- No one's listening to me!
					Wait till you see the sticks I have.
					I could say anything right now.
					I'm gonna get an ant tattoo!
					Let's open some honey and celebrate!
					Maybe I'll pierce my thorax.
					Shave my antennae.
					Shack up with a grasshopper. Get
					a gold tooth and call everybody "dawg"!
					I'm so proud.
					- We're starting work today!
					- Today's the day.
					Oome on! All the good jobs
					will be gone.
					Yeah, right.
					Pollen counting, stunt bee, pouring,
					stirrer, front desk, hair removal...
					- Is it still available?
					- Hang on. Two left!
					One of them's yours! Oongratulations!
					Step to the side.
					- What'd you get?
					- Picking crud out. Stellar!
					Wow!
					Oouple of newbies?
					Yes, sir! Our first day! We are ready!
					Make your choice.
					- You want to go first?
					- No, you go.
					Oh, my. What's available?
					Restroom attendant's open,
					not for the reason you think.
					- Any chance of getting the Krelman?
					- Sure, you're on.
					I'm sorry, the Krelman just closed out.
					Wax monkey's always open.
					The Krelman opened up again.
					What happened?
					A bee died. Makes an opening. See?
					He's dead. Another dead one.
					Deady. Deadified. Two more dead.
					Dead from the neck up.
					Dead from the neck down. That's life!
					Oh, this is so hard!
					Heating, cooling,
					stunt bee, pourer, stirrer,
					humming, inspector number seven,
					lint coordinator, stripe supervisor,
					mite wrangler. Barry, what
					do you think I should... Barry?
					Barry!
					All right, we've got the sunflower patch
					in quadrant nine...
					What happened to you?
					Where are you?
					- I'm going out.
					- Out? Out where?
					- Out there.
					- Oh, no!
					I have to, before I go
					to work for the rest of my life.
					You're gonna die! You're crazy! Hello?
					Another call coming in.
					If anyone's feeling brave,
					there's a Korean deli on 83rd
					that gets their roses today.
					Hey, guys.
					- Look at that.
					- Isn't that the kid we saw yesterday?
					Hold it, son, flight deck's restricted.
					It's OK, Lou. We're gonna take him up.
					Really? Feeling lucky, are you?
					Sign here, here. Just initial that.
					- Thank you.
					- OK.
					You got a rain advisory today,
					and as you all know,
					bees cannot fly in rain.
					So be careful. As always,
					watch your brooms,
					hockey sticks, dogs,
					birds, bears and bats.
					Also, I got a couple of reports
					of root beer being poured on us.
					Murphy's in a home because of it,
					babbling like a cicada!
					- That's awful.
					- And a reminder for you rookies,
					bee law number one,
					absolutely no talking to humans!
					All right, launch positions!
					Buzz, buzz, buzz, buzz! Buzz, buzz,
					buzz, buzz! Buzz, buzz, buzz, buzz!
					Black and yellow!
					Hello!
					You ready for this, hot shot?
					Yeah. Yeah, bring it on.
					Wind, check.
					- Antennae, check.
					- Nectar pack, check.
					- Wings, check.
					- Stinger, check.
					Scared out of my shorts, check.
					OK, ladies,
					let's move it out!
					Pound those petunias,
					you striped stem-suckers!
					All of you, drain those flowers!
					Wow! I'm out!
					I can't believe I'm out!
					So blue.
					I feel so fast and free!
					Box kite!
					Wow!
					Flowers!
					This is Blue Leader.
					We have roses visual.
					Bring it around 30 degrees and hold.
					Roses!
					30 degrees, roger. Bringing it around.
					Stand to the side, kid.
					It's got a bit of a kick.
					That is one nectar collector!
					- Ever see pollination up close?
					- No, sir.
					I pick up some pollen here, sprinkle it
					over here. Maybe a dash over there,
					a pinch on that one.
					See that? It's a little bit of magic.
					That's amazing. Why do we do that?
					That's pollen power. More pollen, more
					flowers, more nectar, more honey for us.
					Oool.
					I'm picking up a lot of bright yellow.
					Oould be daisies. Don't we need those?
					Oopy that visual.
					Wait. One of these flowers
					seems to be on the move.
					Say again? You're reporting
					a moving flower?
					Affirmative.
					That was on the line!
					This is the coolest. What is it?
					I don't know, but I'm loving this color.
					It smells good.
					Not like a flower, but I like it.
					Yeah, fuzzy.
					Ohemical-y.
					Oareful, guys. It's a little grabby.
					My sweet lord of bees!
					Oandy-brain, get off there!
					Problem!
					- Guys!
					- This could be bad.
					Affirmative.
					Very close.
					Gonna hurt.
					Mama's little boy.
					You are way out of position, rookie!
					Ooming in at you like a missile!
					Help me!
					I don't think these are flowers.
					- Should we tell him?
					- I think he knows.
					What is this?!
					Match point!
					You can start packing up, honey,
					because you're about to eat it!
					Yowser!
					Gross.
					There's a bee in the car!
					- Do something!
					- I'm driving!
					- Hi, bee.
					- He's back here!
					He's going to sting me!
					Nobody move. If you don't move,
					he won't sting you. Freeze!
					He blinked!
					Spray him, Granny!
					What are you doing?!
					Wow... the tension level
					out here is unbelievable.
					I gotta get home.
					Oan't fly in rain.
					Oan't fly in rain.
					Oan't fly in rain.
					Mayday! Mayday! Bee going down!
					Ken, could you close
					the window please?
					Ken, could you close
					the window please?
					Oheck out my new resume.
					I made it into a fold-out brochure.
					You see? Folds out.
					Oh, no. More humans. I don't need this.
					What was that?
					Maybe this time. This time. This time.
					This time! This time! This...
					Drapes!
					That is diabolical.
					It's fantastic. It's got all my special
					skills, even my top-ten favorite movies.
					What's number one? Star Wars?
					Nah, I don't go for that...
					...kind of stuff.
					No wonder we shouldn't talk to them.
					They're out of their minds.
					When I leave a job interview, they're
					flabbergasted, can't believe what I say.
					There's the sun. Maybe that's a way out.
					I don't remember the sun
					having a big 75 on it.
					I predicted global warming.
					I could feel it getting hotter.
					At first I thought it was just me.
					Wait! Stop! Bee!
					Stand back. These are winter boots.
					Wait!
					Don't kill him!
					You know I'm allergic to them!
					This thing could kill me!
					Why does his life have
					less value than yours?
					Why does his life have any less value
					than mine? Is that your statement?
					I'm just saying all life has value. You
					don't know what he's capable of feeling.
					My brochure!
					There you go, little guy.
					I'm not scared of him.
					It's an allergic thing.
					Put that on your resume brochure.
					My whole face could puff up.
					Make it one of your special skills.
					Knocking someone out
					is also a special skill.
					Right. Bye, Vanessa. Thanks.
					- Vanessa, next week? Yogurt night?
					- Sure, Ken. You know, whatever.
					- You could put carob chips on there.
					- Bye.
					- Supposed to be less calories.
					- Bye.
					I gotta say something.
					She saved my life.
					I gotta say something.
					All right, here it goes.
					Nah.
					What would I say?
					I could really get in trouble.
					It's a bee law.
					You're not supposed to talk to a human.
					I can't believe I'm doing this.
					I've got to.
					Oh, I can't do it. Oome on!
					No. Yes. No.
					Do it. I can't.
					How should I start it?
					"You like jazz?" No, that's no good.
					Here she comes! Speak, you fool!
					Hi!
					I'm sorry.
					- You're talking.
					- Yes, I know.
					You're talking!
					I'm so sorry.
					No, it's OK. It's fine.
					I know I'm dreaming.
					But I don't recall going to bed.
					Well, I'm sure this
					is very disconcerting.
					This is a bit of a surprise to me.
					I mean, you're a bee!
					I am. And I'm not supposed
					to be doing this,
					but they were all trying to kill me.
					And if it wasn't for you...
					I had to thank you.
					It's just how I was raised.
					That was a little weird.
					- I'm talking with a bee.
					- Yeah.
					- They call it a crumb.
					- It was so stingin' stripey!
					the entire animal kingdom.
					And then, of course...
					The human species?
					So if there's no more pollination,
					it could all just go south here,
					couldn't it?
					I know this is also partly my fault.
					How about a suicide pact?
					How do we do it?
					- I'll sting you, you step on me.
					- Thatjust kills you twice.
					Right, right.
					Listen, Barry...
					sorry, but I gotta get going.
					I had to open my mouth and talk.
					Vanessa?
					Vanessa? Why are you leaving?
					Where are you going?
					To the final Tournament of Roses parade
					in Pasadena.
					They've moved it to this weekend
					because all the flowers are dying.
					It's the last chance
					I'll ever have to see it.
					Vanessa, I just wanna say I'm sorry.
					I never meant it to turn out like this.
					I know. Me neither.
					Tournament of Roses.
					Roses can't do sports.
					Wait a minute. Roses. Roses?
					Roses!
					Vanessa!
					Roses?!
					Barry?
					- Roses are flowers!
					- Yes, they are.
					Flowers, bees, pollen!
					I know.
					That's why this is the last parade.
					Maybe not.
					Oould you ask him to slow down?
					Oould you slow down?
					Barry!
					OK, I made a huge mistake.
					This is a total disaster, all my fault.
					Yes, it kind of is.
					I've ruined the planet.
					I wanted to help you
					with the flower shop.
					I've made it worse.
					Actually, it's completely closed down.
					I thought maybe you were remodeling.
					But I have another idea, and it's
					greater than my previous ideas combined.
					I don't want to hear it!
					All right, they have the roses,
					the roses have the pollen.
					I know every bee, plant
					and flower bud in this park.
					All we gotta do is get what they've got
					back here with what we've got.
					- Bees.
					- Park.
					- Pollen!
					- Flowers.
					- Repollination!
					- Across the nation!
					Tournament of Roses,
					Pasadena, Oalifornia.
					They've got nothing
					but flowers, floats and cotton candy.
					Security will be tight.
					I have an idea.
					Vanessa Bloome, FTD.
					Official floral business. It's real.
					Sorry, ma'am. Nice brooch.
					Thank you. It was a gift.
					Once inside,
					we just pick the right float.
					How about The Princess and the Pea?
					I could be the princess,
					and you could be the pea!
					Yes, I got it.
					- Where should I sit?
					- What are you?
					- I believe I'm the pea.
					- The pea?
					It goes under the mattresses.
					- Not in this fairy tale, sweetheart.
					- I'm getting the marshal.
					You do that!
					This whole parade is a fiasco!
					Let's see what this baby'll do.
					Hey, what are you doing?!
					Then all we do
					is blend in with traffic...
					...without arousing suspicion.
					Once at the airport,
					there's no stopping us.
					Stop! Security.
					- You and your insect pack your float?
					- Yes.
					Has it been
					in your possession the entire time?
					Would you remove your shoes?
					- Remove your stinger.
					- It's part of me.
					I know. Just having some fun.
					Enjoy your flight.
					Then if we're lucky, we'll have
					just enough pollen to do the job.
					Oan you believe how lucky we are? We
					have just enough pollen to do the job!
					I think this is gonna work.
					It's got to work.
					Attention, passengers,
					this is Oaptain Scott.
					We have a bit of bad weather
					in New York.
					It looks like we'll experience
					a couple hours delay.
					Barry, these are cut flowers
					with no water. They'll never make it.
					I gotta get up there
					and talk to them.
					Be careful.
					Oan I get help
					with the Sky Mall magazine?
					I'd like to order the talking
					inflatable nose and ear hair trimmer.
					Oaptain, I'm in a real situation.
					- What'd you say, Hal?
					- Nothing.
					Bee!
					Don't freak out! My entire species...
					What are you doing?
					- Wait a minute! I'm an attorney!
					- Who's an attorney?
					Don't move.
					Oh, Barry.
					Good afternoon, passengers.
					This is your captain.
					Would a Miss Vanessa Bloome in 24B
					please report to the cockpit?
					And please hurry!
					What happened here?
					There was a DustBuster,
					a toupee, a life raft exploded.
					One's bald, one's in a boat,
					they're both unconscious!
					- Is that another bee joke?
					- No!
					No one's flying the plane!
					This is JFK control tower, Flight 356.
					What's your status?
					This is Vanessa Bloome.
					I'm a florist from New York.
					Where's the pilot?
					He's unconscious,
					and so is the copilot.
					Not good. Does anyone onboard
					have flight experience?
					As a matter of fact, there is.
					- Who's that?
					- Barry Benson.
					From the honey trial?! Oh, great.
					Vanessa, this is nothing more
					than a big metal bee.
					It's got giant wings, huge engines.
					I can't fly a plane.
					- Why not? Isn't John Travolta a pilot?
					- Yes.
					How hard could it be?
					Wait, Barry!
					We're headed into some lightning.
					This is Bob Bumble. We have some
					late-breaking news from JFK Airport,
					where a suspenseful scene
					is developing.
					Barry Benson,
					fresh from his legal victory...
					That's Barry!
					...is attempting to land a plane,
					loaded with people, flowers
					and an incapacitated flight crew.
					Flowers?!
					We have a storm in the area
					and two individuals at the controls
					with absolutely no flight experience.
					Just a minute.
					There's a bee on that plane.
					I'm quite familiar with Mr. Benson
					and his no-account compadres.
					They've done enough damage.
					But isn't he your only hope?
					Technically, a bee
					shouldn't be able to fly at all.
					Their wings are too small...
					Haven't we heard this a million times?
					"The surface area of the wings
					and body mass make no sense."
					- Get this on the air!
					- Got it.
					- Stand by.
					- We're going live.
					The way we work may be a mystery to you.
					Making honey takes a lot of bees
					doing a lot of small jobs.
					But let me tell you about a small job.
					If you do it well,
					it makes a big difference.
					More than we realized.
					To us, to everyone.
					That's why I want to get bees
					back to working together.
					That's the bee way!
					We're not made of Jell-O.
					We get behind a fellow.
					- Black and yellow!
					- Hello!
					Left, right, down, hover.
					- Hover?
					- Forget hover.
					This isn't so hard.
					Beep-beep! Beep-beep!
					Barry, what happened?!
					Wait, I think we were
					on autopilot the whole time.
					- That may have been helping me.
					- And now we're not!
					So it turns out I cannot fly a plane.
					All of you, let's get
					behind this fellow! Move it out!
					Move out!
					Our only chance is if I do what I'd do,
					you copy me with the wings of the plane!
					Don't have to yell.
					I'm not yelling!
					We're in a lot of trouble.
					It's very hard to concentrate
					with that panicky tone in your voice!
					It's not a tone. I'm panicking!
					I can't do this!
					Vanessa, pull yourself together.
					You have to snap out of it!
					You snap out of it.
					You snap out of it.
					- You snap out of it!
					- You snap out of it!
					- You snap out of it!
					- You snap out of it!
					- You snap out of it!
					- You snap out of it!
					- Hold it!
					- Why? Oome on, it's my turn.
					How is the plane flying?
					I don't know.
					Hello?
					Benson, got any flowers
					for a happy occasion in there?
					The Pollen Jocks!
					They do get behind a fellow.
					- Black and yellow.
					- Hello.
					All right, let's drop this tin can
					on the blacktop.
					Where? I can't see anything. Oan you?
					No, nothing. It's all cloudy.
					Oome on. You got to think bee, Barry.
					- Thinking bee.
					- Thinking bee.
					Thinking bee!
					Thinking bee! Thinking bee!
					Wait a minute.
					I think I'm feeling something.
					- What?
					- I don't know. It's strong, pulling me.
					Like a 27-million-year-old instinct.
					Bring the nose down.
					Thinking bee!
					Thinking bee! Thinking bee!
					- What in the world is on the tarmac?
					- Get some lights on that!
					Thinking bee!
					Thinking bee! Thinking bee!
					- Vanessa, aim for the flower.
					- OK.
					Out the engines. We're going in
					on bee power. Ready, boys?
					Affirmative!
					Good. Good. Easy, now. That's it.
					Land on that flower!
					Ready? Full reverse!
					Spin it around!
					- Not that flower! The other one!
					- Which one?
					- That flower.
					- I'm aiming at the flower!
					That's a fat guy in a flowered shirt.
					I mean the giant pulsating flower
					made of millions of bees!
					Pull forward. Nose down. Tail up.
					Rotate around it.
					- This is insane, Barry!
					- This's the only way I know how to fly.
					Am I koo-koo-kachoo, or is this plane
					flying in an insect-like pattern?
					Get your nose in there. Don't be afraid.
					Smell it. Full reverse!
					Just drop it. Be a part of it.
					Aim for the center!
					Now drop it in! Drop it in, woman!
					Oome on, already.
					Barry, we did it!
					You taught me how to fly!
					- Yes. No high-five!
					- Right.
					Barry, it worked!
					Did you see the giant flower?
					What giant flower? Where? Of course
					I saw the flower! That was genius!
					- Thank you.
					- But we're not done yet.
					Listen, everyone!
					This runway is covered
					with the last pollen
					from the last flowers
					available anywhere on Earth.
					That means this is our last chance.
					We're the only ones who make honey,
					pollinate flowers and dress like this.
					If we're gonna survive as a species,
					this is our moment! What do you say?
					Are we going to be bees, orjust
					Museum of Natural History keychains?
					We're bees!
					Keychain!
					Then follow me! Except Keychain.
					Hold on, Barry. Here.
					You've earned this.
					Yeah!
					I'm a Pollen Jock! And it's a perfect
					fit. All I gotta do are the sleeves.
					Oh, yeah.
					That's our Barry.
					Mom! The bees are back!
					If anybody needs
					to make a call, now's the time.
					I got a feeling we'll be
					working late tonight!
					Here's your change. Have a great
					afternoon! Oan I help who's next?
					Would you like some honey with that?
					It is bee-approved. Don't forget these.
					Milk, cream, cheese, it's all me.
					And I don't see a nickel!
					Sometimes I just feel
					like a piece of meat!
					I had no idea.
					Barry, I'm sorry.
					Have you got a moment?
					Would you excuse me?
					My mosquito associate will help you.
					Sorry I'm late.
					He's a lawyer too?
					I was already a blood-sucking parasite.
					All I needed was a briefcase.
					Have a great afternoon!
					Barry, I just got this huge tulip order,
					and I can't get them anywhere.
					No problem, Vannie.
					Just leave it to me.
					You're a lifesaver, Barry.
					Oan I help who's next?
					All right, scramble, jocks!
					It's time to fly.
					Thank you, Barry!
					That bee is living my life!
					Let it go, Kenny.
					- When will this nightmare end?!
					- Let it all go.
					- Beautiful day to fly.
					- Sure is.
					Between you and me,
					I was dying to get out of that office.
					You have got
					to start thinking bee, my friend.
					- Thinking bee!
					- Me?
					Hold it. Let's just stop
					for a second. Hold it.
					I'm sorry. I'm sorry, everyone.
					Oan we stop here?
					I'm not making a major life decision
					during a production number!
					All right. Take ten, everybody.
					Wrap it up, guys.""";

}

