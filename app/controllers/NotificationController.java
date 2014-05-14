package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Account;
import models.Notification;
import play.Logger;
import play.api.templates.Html;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;
import scala.Array;
import scala.collection.mutable.StringBuilder;

@Transactional
@Security.Authenticated(Secured.class)
public class NotificationController extends BaseController{
	
	public static Html view() {
		return getNotifications();
	}
	
	public static Result viewAjax() {
		return ok(getNotifications());
	}
	
	private static Html getNotifications() {
		Account account = Component.currentAccount();
		
		if(account == null) {
			return new Html(new StringBuilder("Das wird nichts"));
		}
		
		List<Notification> list = new ArrayList<Notification>();
		if(Notification.countForAccount(account) >= 0) {
			list = Notification.findByUser(account);			
		} 
		return views.html.Notification.menuitem.render(list);
	}
	
	public static Result forward(Long notificationId, String url) {
		Notification note = Notification.findById(notificationId);
		Logger.info(url);

		if(note == null) {
			return badRequest("Das gibts doch garnicht!");
		}
		
		if(!Secured.deleteNotification(note)) {
			return redirect(routes.Application.index());
		}
		
		note.delete();		
		return redirect(url);
	}
	
	public static Result deleteAll() {
		Notification.deleteByUser(Component.currentAccount());
		flash("success", "Alle Neuigkeiten wurden gelöscht.");
		return ok();
	}
	
	
	
}
