package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.i18n.Messages;
import play.mvc.Http.Context;
import play.mvc.*;

public class Navigation {

	public static enum Level {PROFILE,STREAM,FRIENDS,GROUPS,COURSES,HELP,USER,ADMIN,NOTIFICATIONS}
	
	private static Map<Level,Call> callMapping = new HashMap<Navigation.Level, Call>();
	static
	{
		callMapping.put(Level.PROFILE, controllers.routes.ProfileController.me());
		callMapping.put(Level.STREAM, controllers.routes.Application.index());
		callMapping.put(Level.FRIENDS, controllers.routes.FriendshipController.index());
		callMapping.put(Level.GROUPS, controllers.routes.GroupController.index());
		callMapping.put(Level.HELP, controllers.routes.Application.help());
		callMapping.put(Level.ADMIN, controllers.routes.AdminController.index());
        callMapping.put(Level.NOTIFICATIONS, controllers.routes.NotificationController.showAll(1));
	}
	
	private static Map<Level,String> titleMapping = new HashMap<Navigation.Level, String>();
	static
	{
		titleMapping.put(Level.PROFILE, "Profil");
		titleMapping.put(Level.STREAM, "Newsstream");
		titleMapping.put(Level.FRIENDS, "Freunde");
		titleMapping.put(Level.GROUPS, "Gruppen & Kurse");
		titleMapping.put(Level.HELP, "Hilfe");
		titleMapping.put(Level.USER, "Person");
		titleMapping.put(Level.ADMIN, "Control Center");
        titleMapping.put(Level.NOTIFICATIONS, Messages.get("notification.news"));
	}
	
	private static Call fallbackCall = controllers.routes.Application.index();

	final private static String levelIdent = "navLevel";
	final private static String titleIdent = "navTitle";
	final private static String parentTitleIdent = "navParentTitle";
	final private static String parentCallIdent = "navParentCall";
	
	public static void set(Level level) {
		Navigation.set(level,null,null,null);
	}
	
	public static void set(String title) {
		Navigation.set(null,title,null,null);
	}
	
	public static void set(Level level, String title) {
		Navigation.set(level,title,null,null);
	}
	
	public static void set(Level level, String title, String parentTitle, Call parentCall) {
		Context ctx = Context.current();
		ctx.args.put(levelIdent, level);
		ctx.args.put(titleIdent, title);
		ctx.args.put(parentTitleIdent, parentTitle);
		ctx.args.put(parentCallIdent, parentCall);
	}
	
	public static Level getLevel() {
		return (Level)Context.current().args.get(levelIdent);
	}
	
	public static Call getLevelRoute(Level level) {
		if(level != null) {
			if(callMapping.containsKey(level)){
				return callMapping.get(level);
			} else {
				return fallbackCall;
			}
		} else {
			return fallbackCall;
		}	
	}
	
	public static String getLevelTitle(Level level) {
		if(level != null) {
			if(titleMapping.containsKey(level)){
				return titleMapping.get(level);
			} else {
				return "UNKNOWN";
			}
		} else {
			return "UNKNOWN";
		}	
	}
	
	public static String getTitle() {
		return (String)Context.current().args.get(titleIdent);
	}
	
	public static String getParentTitle() {
		return (String)Context.current().args.get(parentTitleIdent);
	}
	
	public static Call getParentCall() {
		return (Call)Context.current().args.get(parentCallIdent);
	}
		
	public static Map<String, Object> calcPagination(int count, int limit, int page) {
		Map<String, Object> m = new HashMap<String, Object>();
		Boolean first = true;
		Boolean last = true;
		int lastPage = (int)Math.ceil(count/(double)limit);

		if(lastPage == 0){
			lastPage = 1;
		}
		
		if(page < 1) {
			page = 1;
		} else if(page > lastPage) {
			page = lastPage;
		}
		
		List<Integer> pages = new ArrayList<Integer>();
		int show = 5; // SHOULD BE ODD!
		int edge = lastPage - show + 1;
		
		if(page < show){
			for(int i=0;i<show;i++){
				if(i == lastPage){
					break;
				}
				pages.add(i+1);
			}
			first = false;
		} else if (page > edge) {
			for(int i=edge;i<=lastPage;i++){
				pages.add(i);
			}
			first = true;
			last = false;
		} else if (page >= show) {
			int pitch = show / 2;
			for(int i=0;i<show;i++){
				pages.add(page - pitch + i);
			}
			first = true;
			last = true;
		}
		
		if(lastPage <= show) {
			first = false;
			last = false;
		}
		
		Boolean firstDots = false;
		Boolean lastDots = false;
		

		if(pages.get(0) - 1 !=1 ){
			firstDots = true;
		}

		if(lastPage != pages.get(pages.size()-1) + 1){
			lastDots = true;
		}

		m.put("first", first);
		m.put("last", last);
		m.put("firstDots", firstDots);
		m.put("lastDots", lastDots);
		m.put("lastPage", lastPage);
		m.put("pages", pages);
		return m;
	}
	
	
}
