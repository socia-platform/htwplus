package controllers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletionStage;

import managers.AccountManager;
import models.Account;
import play.Logger;
import play.twirl.api.Html;
import play.db.jpa.Transactional;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

import javax.inject.Inject;

public class Component extends Action.Simple {

    private final AccountManager accountManager;

    @Inject
    public Component(AccountManager accountManager) {
        this.accountManager = accountManager;
    }
	
	@Override
	@Transactional
    public CompletionStage<Result> call(Context ctx) {
		String sessionId = ctx.session().get("id");
		if(sessionId != null) {
			Long id = Long.parseLong(ctx.session().get("id"));
			Account account = accountManager.findById(id);
			if(account == null) {
				ctx.session().clear();
				Logger.info("Clear Session");
				return delegate.call(ctx);
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