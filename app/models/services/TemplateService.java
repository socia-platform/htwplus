package models.services;

import org.joda.time.DateTime;
import org.joda.time.Days;
import play.Logger;
import play.i18n.Messages;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Helper service to render templates. This service also provides helper methods to use in templates.
 */
public class TemplateService {
    /**
     * Singleton instance
     */
    private static TemplateService instance = null;

    /**
     * Private constructor for singleton instance
     */
    private TemplateService() { }

    /**
     * Returns the singleton instance.
     *
     * @return EmailHandler instance
     */
    public static TemplateService getInstance() {
        if (TemplateService.instance == null) {
            TemplateService.instance = new TemplateService();
        }

        return TemplateService.instance;
    }

    /**
     * Returns the classes as array of the template parameters as this information
     * is required for method reflection.
     *
     * @param templateParameters Template parameters
     * @return Array of classes
     */
    protected Class[] getParameterClasses(Object... templateParameters) {
        Class[] parameterClasses = new Class[templateParameters.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            // template render methods usually use List interfaces, therefore we need to add List.class instead of
            // realizing classes like ArrayList which might be given in templateParameters
            if (templateParameters[i] instanceof List) {
                parameterClasses[i] = List.class;
            } else {
                parameterClasses[i] = templateParameters[i].getClass();
            }
        }

        return parameterClasses;
    }

    /**
     * Renders a HTML template and returns the content as a String. If an exception appears (e.g. template
     * path is not valid), an empty String is returned.
     *
     * @param templatePath Template path
     * @param templateParameters Template parameters
     * @return Rendered content as String or empty String on exception
     */
    public String getRenderedTemplate(String templatePath, Object... templateParameters) {
        try {
            // determine class of template, retrieve render method of template and invoke render()
            Class<?> templateClass = Class.forName(templatePath);
            Method renderMethod = templateClass.getDeclaredMethod("render", this.getParameterClasses(templateParameters));

            return renderMethod.invoke(null, templateParameters).toString().trim();
        } catch (ClassNotFoundException e) {
            Logger.error("Could not get template class for template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (NoSuchMethodException e) {
            Logger.error("Could not get render method for template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (InvocationTargetException e) {
            Logger.error("Invocation exception while render() template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (IllegalAccessException e) {
            Logger.error("IllegalAccessException exception while render() template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (Exception e) {
            Logger.error("Exception while trying to render template path: " + templatePath + "(" + e.getMessage() + ")");
        }

        // previous exception is logged, return empty String
        return "";
    }

    /**
     * Get a (correct) difference between two dates using Joda-Time API.
     * See: http://stackoverflow.com/a/1555307
     * See: http://www.joda.org/joda-time/
     *
     * @param date1 the oldest date
     * @param date2 the newest date
     * @return The difference in days
     */
    public static int getDateDifference(Date date1, Date date2) {
        DateTime dateTime1 = new DateTime(date1).withTimeAtStartOfDay();
        DateTime dateTime2 = new DateTime(date2).withTimeAtStartOfDay();

        return Days.daysBetween(dateTime1, dateTime2).getDays();
    }

    /**
     * Returns a colloquially date from a date instance.
     *
     * @param date Date instance
     * @return Colloquially date
     */
    public static String getDateColloquially(Date date) {
        long dateDifference = TemplateService.getDateDifference(date, new Date());
        SimpleDateFormat dateFormatTime = new SimpleDateFormat("HH:mm");

        if (dateDifference > 7) {
            SimpleDateFormat dateFormatDate = new SimpleDateFormat("dd.MM.yyyy");
            return Messages.get("post.date_colloquially_date", dateFormatDate.format(date), dateFormatTime.format(date));
        } else if (dateDifference > 2) {
            return Messages.get("post.date_colloquially_days", dateDifference, dateFormatTime.format(date));
        } else if (dateDifference > 1) {
            return Messages.get("post.date_colloquially_day_before_yesterday", dateFormatTime.format(date));
        } else if (dateDifference > 0) {
            return Messages.get("post.date_colloquially_yesterday", dateFormatTime.format(date));
        } else {
            return Messages.get("post.date_colloquially_today", dateFormatTime.format(date));
        }
    }
}
