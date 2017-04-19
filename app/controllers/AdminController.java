package controllers;

import managers.*;
import models.Account;
import models.Folder;
import models.Post;
import models.enums.AccountRole;
import models.services.ElasticsearchService;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.IndexNotFoundException;
import play.Logger;
import play.api.i18n.Lang;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.Transactional;
import play.i18n.MessagesApi;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;
import views.html.Admin.*;

import javax.inject.Inject;
import java.io.IOException;

@Security.Authenticated(Secured.class)
@With(AdminAction.class)
public class AdminController extends BaseController {

    private final ElasticsearchService elasticsearchService;
    private final MediaManager mediaManager;
    private final GroupManager groupManager;
    private final PostManager postManager;
    private final AccountManager accountManager;
    private final FolderManager folderManager;
    private final MessagesApi messagesApi;
    private final FormFactory formFactory;
    private final Form<Account> accountForm;
    private final Form<Post> postForm;

    @Inject
    public AdminController(ElasticsearchService elasticsearchService,
                           MediaManager mediaManager,
                           GroupManager groupManager,
                           PostManager postManager,
                           AccountManager accountManager,
                           FolderManager folderManager,
                           FormFactory formFactory,
                           MessagesApi messagesApi) {
        this.elasticsearchService = elasticsearchService;
        this.mediaManager = mediaManager;
        this.groupManager = groupManager;
        this.postManager = postManager;
        this.accountManager = accountManager;
        this.folderManager = folderManager;
        this.messagesApi = messagesApi;
        this.formFactory = formFactory;
        this.accountForm = formFactory.form(Account.class);
        this.postForm =  formFactory.form(Post.class);
    }



    public Result index() {
        return ok(index.render());
    }

    public Result createAccountForm() {
        return ok(createAccount.render(accountForm));
    }

    @Transactional
    public Result createAccount() {
        Form<Account> filledForm = accountForm.bindFromRequest();
        Logger.info(filledForm.errors().toString());

        filledForm.errors().remove("role");

        if (filledForm.data().get("email").isEmpty()) {
            filledForm.reject("email", "Bitte gib hier etwas ein!");
        }

        if (!(accountManager.findByEmail(filledForm.data().get("email")) == null)) {
            filledForm.reject("email", "Diese Email-Adresse wird bereits verwendet!");
        }

        if (!filledForm.data().get("password").equals(filledForm.data().get("repeatPassword"))) {
            filledForm.reject("repeatPassword", "Passwörter stimmen nicht überein");
        }

        if (filledForm.data().get("password").length() < 6) {
            filledForm.reject("password", "Das Passwort muss mindestens 6 Zeichen haben.");
        }

        if (filledForm.hasErrors()) {
            return badRequest(createAccount.render(filledForm));
        }
        Account account = new Account();
        account.firstname = filledForm.data().get("firstname");
        account.lastname = filledForm.data().get("lastname");
        account.email = filledForm.data().get("email");
        account.password = Component.md5(filledForm.data().get("password"));
        account.avatar = "1";
        account.role = AccountRole.values()[Integer.parseInt(filledForm.data().get("role"))];
        accountManager.create(account);

        flash("success", "User angelegt");
        return ok(createAccount.render(accountForm));

    }

    @Transactional
    public Result deleteAccount(Long accountId) {
        Account current = accountManager.findById(accountId);

        if (!Secured.deleteAccount(current)) {
            flash("error", messagesApi.get(Lang.defaultLang(), "profile.delete.nopermission"));
            return redirect(controllers.routes.AdminController.listAccounts());
        }

        DynamicForm df = formFactory.form().bindFromRequest();
        if (!df.get("confirmText").toLowerCase().equals("account wirklich löschen")) {
            flash("error", messagesApi.get(Lang.defaultLang(), "admin.delete_account.wrongconfirm"));
            return redirect(controllers.routes.AdminController.listAccounts());
        }

        // ACTUAL DELETION //
        Logger.info("Deleting Account[#" + current.id + "]...");
        accountManager.delete(current);

        // override logout message
        flash("success", messagesApi.get(Lang.defaultLang(), "admin.delete_account.success"));
        return redirect(routes.AdminController.listAccounts());
    }

    public Result indexing() {
        return ok(indexing.render());
    }

    public Result indexDelete() {
        try {
            elasticsearchService.deleteIndex();
            flash("info", "index gelöscht");
        } catch (NoNodeAvailableException nna) {
            flash("error", nna.getMessage());
        } catch (IndexNotFoundException infe) {
            flash("error", infe.getMessage());
        }

        return ok(indexing.render());
    }

    public Result indexSettings() throws IOException {
        if (elasticsearchService.isClientAvailable()) {
            if (!elasticsearchService.isIndexExists()) {
                elasticsearchService.createAnalyzer();
                elasticsearchService.createMapping();
                flash("success", "Mapping und Anazyler erfolgreich erstellt!");
            } else {
                flash("error", "Index bereits vorhanden.");
            }
        } else {
            flash("error", "Elasticsearch nicht erreichbar!");
        }

        return ok(indexing.render());
    }

    public Result indexAccounts() throws IOException {
        long time = accountManager.indexAllAccounts();
        String out = "Alle Accounts indexiert (" + Long.toString(time) + " Sekunden)";
        flash("info", out);
        return ok(indexing.render());
    }

    public Result indexGroups() throws IOException {
        long time = groupManager.indexAllGroups();
        String out = "Alle Gruppen indexiert (" + Long.toString(time) + " Sekunden)";
        flash("info", out);
        return ok(indexing.render());
    }

    public Result indexPosts() throws IOException {
        long time = postManager.indexAllPosts();
        String out = "Alle Posts indexiert (" + Long.toString(time) + " Sekunden)";
        flash("info", out);
        return ok(indexing.render());
    }

    public Result indexMedia() throws IOException {
        long time = mediaManager.indexAllMedia();
        String out = "Alle Media indexiert (" + Long.toString(time) + " Sekunden)";
        flash("info", out);
        return ok(indexing.render());
    }

    public Result viewMediaTemp() {
        //https://issues.apache.org/jira/browse/IO-373
        //String size = FileUtils.byteCountToDisplaySize(MediaController.sizeTemp());

        return ok(mediaTemp.render());
    }

    public Result cleanMediaTemp() {
        mediaManager.cleanUpTemp();
        flash("success", "Media Temp directory was cleaned.");
        return viewMediaTemp();
    }

    public Result listAccounts() {
        return ok(listAccounts.render(accountManager.all()));
    }

    /**
     * Returns the rendered form for broadcast posts.
     *
     * @return Result

    @Transactional
    public Result broadcastNotificationForm() {
        if (!Secured.isAdmin()) {
            return redirect(controllers.routes.Application.index());
        }
        return ok(createBroadcastNotification.render(AdminController.postForm, accountManager.all()));
    }

    /**
     * Handles posted broadcast post. As broadcastMemberList could be a large list and causes
     * Account.getAccountListByIdCollection() to be costly, this action method is realized
     * asynchronous to be non blocking.
     *
     * @return Result

    @Transactional
    public Promise<Result> broadcastNotification() {

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
                            return broadcastNotificationForm();
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
                            recipientList = accountManager.getAccountListByIdCollection(broadcastMemberList);
                        } else {
                            recipientList = accountManager.all();
                        }


                        // add recipients to broadcast post recipient list
                        for (Account account : recipientList) {
                            // add account ID if not the sender
                            if (!broadcastPost.owner.id.equals(account.id)) {
                                broadcastPost.addRecipient(account);
                            }
                        }

                        postManager.create(broadcastPost);

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
    */

    @Transactional
    public Result refactor() {

        for (Account account : accountManager.all()) {
            account.rootFolder = new Folder("_" + account.name, account, null, null, account);
            folderManager.create(account.rootFolder);
            accountManager.update(account);
        }
        return ok();
    }
}
