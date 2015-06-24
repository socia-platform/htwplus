package controllers;

import play.db.jpa.Transactional;
import play.mvc.*;

@Transactional
@With(Component.class)
public class BaseController extends Controller {

    public static Result statusWithWarning(int status, String warning) {
        response().setHeader("Warning", warning);
        return status(status);
    }

}