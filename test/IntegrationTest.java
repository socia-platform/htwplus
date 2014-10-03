import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.*;

import play.Configuration;
import play.test.*;
import play.libs.F.*;

import java.io.File;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

/**
 * Simple integration test.
 */
public class IntegrationTest {
    /**
     * add your integration test here
     * in this example we just check if the welcome page is being shown
     */
    @Test
    public void test() {
        Config config = ConfigFactory.parseFile(new File("conf/application.conf"));
        Configuration additionalConfigurations = new Configuration(config);

        running(testServer(3333, fakeApplication(additionalConfigurations.asMap())), HTMLUNIT, new Callback<TestBrowser>() {
            @Override
            public void invoke(TestBrowser browser) throws Throwable {
                browser.goTo("http://localhost:3333");
                assertThat(browser.pageSource()).contains("Anmelden");
            }
        });
    }
}
