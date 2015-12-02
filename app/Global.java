import java.util.concurrent.TimeUnit;

import models.services.ElasticsearchService;
import models.services.EmailService;

import controllers.MediaController;
import controllers.routes;
import models.Account;
import models.Group;
import models.Post;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import play.Play;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Akka;
import play.libs.F.*;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import scala.concurrent.duration.Duration;

@SuppressWarnings("unused")
public class Global extends GlobalSettings {
	
	@Override
	public void beforeStart(Application app) {
		Logger.info(" Global beforeStart");
		super.beforeStart(app);
	}

	@Override
	public void onStart(Application app) {
		Logger.info("Application - onStart: "+"Prod? "+Play.isProd()+" Test? "+Play.isTest()+" Dev? "+Play.isDev());
		super.onStart(app);

        // Sets the schedule for cleaning the media temp directory
		Akka.system().scheduler().schedule(
            Duration.create(0, TimeUnit.MILLISECONDS),
            Duration.create(30, TimeUnit.MINUTES),
            new Runnable() {
                public void run() {
                    MediaController.cleanUpTemp();
                }
            },
            Akka.system().dispatcher()
        );

        // set the email schedule to next full hour clock for sending daily and hourly emails
        Akka.system().scheduler().schedule(
            Duration.create(nextExecutionInSeconds(getNextHour(), 0), TimeUnit.SECONDS),
            Duration.create(1, TimeUnit.HOURS),
            () -> {
                EmailService email = Play.application().injector().instanceOf(EmailService.class);
                email.sendDailyHourlyNotificationsEmails();
            },
            Akka.system().dispatcher()
        );
	}

    @Override
    public void onStop(Application app) {
        Logger.info("closing ES client...");
        ElasticsearchService elasticsearchService = Play.application().injector().instanceOf(ElasticsearchService.class);
        elasticsearchService.closeClient();
        Logger.info("ES client closed");
    }

    /**
     * Returns the next full hour of the current time
     *
     * @return Next hour of current time
     */
    public static int getNextHour() {
        return (new DateTime()).plusHours(1).getHourOfDay();
    }

    /**
     * Calculates seconds between now and a time for hour and minute.
     *
     * @param hour Hour
     * @param minute Minute
     * @return Seconds
     */
    public static int nextExecutionInSeconds(int hour, int minute) {
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
    public static DateTime nextExecution(int hour, int minute) {
        DateTime next = new DateTime()
                .withHourOfDay(hour)
                .withMinuteOfHour(minute)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        return (next.isBeforeNow())
                ? next.plusHours(24)
                : next;
    }
	
	@Override
	public Promise<Result> onError(final RequestHeader rh, final Throwable t) {
		Logger.error("onError "+ rh + " " + t);
				
		JPA.withTransaction(new play.libs.F.Callback0() {
			@Override
			public void invoke() throws Throwable {
                Group group = Group.findByTitle(play.Play.application().configuration().getString("htwplus.admin.group"));
                if(group != null){
                    Post p = new Post();
                    p.content = "Request: "+rh+"\nError: "+t;
                    p.owner = Account.findByEmail(play.Play.application().configuration().getString("htwplus.admin.mail"));
                    p.group = group;
                    p.create();
                }
			}
		});
		
		
		// prod mode? return 404 page
		if(Play.isProd()){
			return Promise.pure(play.mvc.Results.redirect(routes.Application.error()));
		}

		return super.onError(rh, t);
	}
}
