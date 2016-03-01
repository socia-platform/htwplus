package services;

import akka.actor.ActorSystem;
import managers.MediaManager;
import models.services.EmailService;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import play.DefaultApplication;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Created by Iven on 08.12.2015.
 */
@Singleton
public class ScheduleService {

    private EmailService emailService;
    private MediaManager mediaManager;
    private ActorSystem system;

    @Inject
    public ScheduleService(DefaultApplication app) {
        system = ActorSystem.create();
        this.emailService = app.injector().instanceOf(EmailService.class);
        this.mediaManager = app.injector().instanceOf(MediaManager.class);
        schedule();
    }

    private void schedule() {

        // set the email schedule to next full hour clock for sending daily and hourly emails
        system.scheduler().schedule(
                Duration.create(nextExecutionInSeconds(getNextHour(), 0), TimeUnit.SECONDS),
                Duration.create(1, TimeUnit.HOURS),
                () -> {
                    emailService.sendDailyHourlyNotificationsEmails();
                },
                system.dispatcher()
        );

        // Sets the schedule for cleaning the media temp directory
        system.scheduler().schedule(
                Duration.create(0, TimeUnit.MILLISECONDS),
                Duration.create(2, TimeUnit.HOURS),
                () -> {
                    mediaManager.cleanUpTemp();
                },
                system.dispatcher()
        );
    }

    /**
     * Returns the next full hour of the current time
     *
     * @return Next hour of current time
     */
    private int getNextHour() {
        return (new DateTime()).plusHours(1).getHourOfDay();
    }

    /**
     * Calculates seconds between now and a time for hour and minute.
     *
     * @param hour Hour
     * @param minute Minute
     * @return Seconds
     */
    private int nextExecutionInSeconds(int hour, int minute) {
        return Seconds.secondsBetween(
                new DateTime(),
                nextExecution(hour, minute)
        ).getSeconds();
    }

    /**
     * Returns a DateTime for hour and minute.
     *
     * @param hour Hour
     * @param minute Minute
     * @return DateTime
     */
    private DateTime nextExecution(int hour, int minute) {
        DateTime next = new DateTime()
                .withHourOfDay(hour)
                .withMinuteOfHour(minute)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        return (next.isBeforeNow())
                ? next.plusHours(24)
                : next;
    }
}
