package controllers;

import static play.data.Form.form;

import models.Account;
import models.Group;
import models.Post;
import models.enums.AccountRole;
import models.services.ElasticsearchService;
import models.services.NotificationService;
import org.elasticsearch.action.index.IndexResponse;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

import org.elasticsearch.client.Client;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.F;
import play.mvc.Security;
import play.mvc.Result;
import play.mvc.With;
import views.html.Admin.*;
import play.libs.F.Promise;

import java.io.IOException;
import java.util.*;

@Security.Authenticated(Secured.class)
// Action performs the authentication
@With(AdminAction.class)
public class AdminController extends BaseController {

	static Form<Account> accountForm = form(Account.class);
    static Form<Post> postForm = form(Post.class);
    static Client client = ElasticsearchService.getInstance().getClient();

	public static Result index(){
		return ok(index.render());
	}

	public static Result createAccountForm(){
		return ok(createAccount.render(accountForm));
	}

	@Transactional
	public static Result createAccount() {
		Form<Account> filledForm = accountForm.bindFromRequest();
		Logger.info(filledForm.errors().toString());

		filledForm.errors().remove("role");

		if(filledForm.data().get("email").isEmpty()) {
			filledForm.reject("email", "Bitte gib hier etwas ein!");
		}

		if (!(Account.findByEmail(filledForm.data().get("email")) == null)) {
			filledForm.reject("email", "Diese Email-Adresse wird bereits verwendet!");
		}

		if (!filledForm.data().get("password").equals(filledForm.data().get("repeatPassword"))) {
			filledForm.reject("repeatPassword", "Passwörter stimmen nicht überein");
		}

		if (filledForm.data().get("password").length() < 6) {
			filledForm.reject("password", "Das Passwort muss mindestens 6 Zeichen haben.");
		}

		if(filledForm.hasErrors()) {
			return badRequest(createAccount.render(filledForm));
		}

		Account a = new Account();
		a.firstname = filledForm.data().get("firstname");
		a.lastname = filledForm.data().get("lastname");
		a.email = filledForm.data().get("email");
		a.password = Component.md5(filledForm.data().get("password"));
		a.avatar = "a1";
		a.role = AccountRole.values()[Integer.parseInt(filledForm.data().get("role"))];
		a.create();

		flash("success", "User angelegt");
		return ok(createAccount.render(accountForm));

	}

    public static Result indexing() {
        return ok(indexing.render());
    }

    public static Result indexAccounts() throws IOException {
        long time = Account.indexAllAccounts();
        String out = "All Accounts indexed. It took "+Long.toString(time)+"ms";
        Logger.info(out);
        flash("info",out);
        return ok(indexing.render());
    }

    public static Result indexGroups() throws IOException {
        long time = Group.indexAllGroups();
        String out = "All Groups indexed. It took "+Long.toString(time)+"ms";
        Logger.info(out);
        flash("info",out);
        return ok(indexing.render());
    }

    public static Result indexPosts() throws IOException {
        long time = Post.indexAllPosts();
        String out = "All Posts indexed. It took "+Long.toString(time)+"ms";
        Logger.info(out);
        flash("info",out);
        return ok(indexing.render());
    }

	public static Result viewMediaTemp() {
		//https://issues.apache.org/jira/browse/IO-373
		//String size = FileUtils.byteCountToDisplaySize(MediaController.sizeTemp());

		long bytes = MediaController.sizeTemp();
		String size = (bytes > 0) ? MediaController.bytesToString(bytes, false) : "keine Daten vorhanden";
		return ok(mediaTemp.render(size));
	}

	public static Result cleanMediaTemp(){
		MediaController.cleanUpTemp();
		flash("success", "Media Temp directory was cleaned.");
		return viewMediaTemp();
	}

	public static Result listAccounts(){
		return ok(listAccounts.render(Account.all()));
	}

    /**
     * Returns the rendered form for broadcast posts.
     *
     * @return Result
     */
    @Transactional
    public static Result broadcastNotificationForm() {
        if (!Secured.isAdmin()) {
            return redirect(controllers.routes.Application.index());
        }

        return ok(createBroadcastNotification.render(AdminController.postForm, Account.all()));
    }

    /**
     * Handles posted broadcast post. As broadcastMemberList could be a large list and causes
     * Account.getAccountListByIdCollection() to be costly, this action method is realized
     * asynchronous to be non blocking.
     *
     * @return Result
     */
    public static Promise<Result> broadcastNotification() {
        Promise<Result> promiseResult = Promise.promise(
            new F.Function0<Result>() {
                public Result apply() {
                    if (!Secured.isAdmin()) {
                        return redirect(controllers.routes.Application.index());
                    }

                    DynamicForm form = Form.form().bindFromRequest();
                    List<String> broadcastMemberList = new ArrayList<>();
                    final Post broadcastPost = new Post();
                    broadcastPost.owner = Component.currentAccount();
                    broadcastPost.isBroadcastMessage = true;

                    String broadcastMessage = form.data().get(Messages.get("admin.broadcast_notification")).trim();
                    if (broadcastMessage.equals("")) {
                        flash("error", Messages.get("admin.broadcast_notification.error.no_message"));
                        return controllers.AdminController.broadcastNotificationForm();
                    }

                    broadcastPost.content = broadcastMessage;

                    // iterate over posted values to get recipient account IDs and post content, if at least one is clicked
                    // otherwise take all accounts
                    final List<Account> recipientList;
                    if (form.data().size() > 1) {
                        for (Map.Entry<String, String> entry : form.data().entrySet()) {
                            if (entry.getKey().startsWith("account")) {
                                broadcastMemberList.add(entry.getValue());
                            }
                        }
                        recipientList = Account.getAccountListByIdCollection(broadcastMemberList);
                    } else {
                        recipientList = Account.all();
                    }


	                // add recipients to broadcast post recipient list
	                for (Account account : recipientList) {
	                    // add account ID if not the sender
	                    if (!broadcastPost.owner.id.equals(account.id)) {
	                        broadcastPost.addRecipient(account);
	                    }
	                }

	                broadcastPost.create();

                    NotificationService.getInstance().createNotification(broadcastPost, Post.BROADCAST);

                    flash("success", Messages.get("admin.broadcast_notification.success"));
                    return ok(index.render());
                }
            }
        );

        return promiseResult.map(
                new F.Function<Result, Result>() {
                    public Result apply(Result result) {
                        return result;
                    }
                }
        );
    }
}
