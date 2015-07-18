import models.services.TemplateService;
import org.junit.Test;

import java.util.Date;
import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for TemplateService.
 */
public class TemplateTest {
    /**
     * Tests, if an invalid template path results in an empty string.
     */
    @Test
    public void testInvalidTemplate() {
        assertThat(TemplateService.getInstance().getRenderedTemplate("does.not.exists")).isEqualToIgnoringCase("");
    }

    /**
     * Tests, if a valid template path results in a rendered string including parameters.
     */
    @Test
    public void testValidTemplate() {
        Date date = new Date();
        int random = (new Random()).nextInt();

        String renderedTest = TemplateService.getInstance()
                .getRenderedTemplate("views.html.Test.template_test", date, random);
        assertThat(renderedTest).contains("TEMPLATE TEST");
        assertThat(renderedTest).contains(String.valueOf(date.getTime()));
        assertThat(renderedTest).contains(String.valueOf(random));
    }
}
