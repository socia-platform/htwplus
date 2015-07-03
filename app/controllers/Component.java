package controllers;

import static play.data.Form.form;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import models.Account;
import models.Login;
import play.Logger;
import play.twirl.api.Html;
import play.data.Form;
import play.db.jpa.Transactional;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

public class Component extends Action.Simple {
	
	@Override
	@Transactional
    public Promise<Result> call(Context ctx) throws Throwable {
		String sessionId = ctx.session().get("id");
		if(sessionId != null) {
			Long id = Long.parseLong(ctx.session().get("id"));
			Account account = Account.findById(id);
			if(account == null) {
				ctx.session().clear();
				Logger.info("Clear Session");
				return Promise.pure(redirect(controllers.routes.Application.index()));
			} 
			ctx.args.put("account", account);
		} else {
			ctx.args.put("account", null);
		}
		
        return delegate.call(ctx);
    }
	
    public static class ContextIdent {
        public static String loginForm = "loginForm";
    }
    
    public static void addToContext(String ident, Object object) {
        Context.current().args.put(ident, object);
    }

    public static Object getFromContext(String ident) {
        return Context.current().args.get(ident);
    }

    public static Account currentAccount() {
        return (Account)Context.current().args.get("account");
    }
    
    @SuppressWarnings("unchecked")
    public static Html loginForm() {
        Form<Login> form = form(Login.class);
        if (Component.getFromContext(ContextIdent.loginForm) != null) {
            form = (Form<Login>) Component.getFromContext(ContextIdent.loginForm);
        }
        return views.html.snippets.loginForm.render(form);
    }
    
    /**
     * Generates an md5 hash of a String.
     * @param input String value
     * @return Hashvalue of the String.
     */
    public static String md5(String input) {
        
        String md5 = null;
         
        if(null == input) return null;
         
        try {
            //Create MessageDigest object for MD5
            MessageDigest digest = MessageDigest.getInstance("MD5");

            //Update input string in message digest
            digest.update(input.getBytes(), 0, input.length());

            //Converts message digest value in base 16 (hex)
            md5 = new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }
    
}