package controllers;

import play.db.jpa.Transactional;
import play.mvc.*;

@Transactional
@With(Component.class)
public class BaseController extends Controller {
	

}