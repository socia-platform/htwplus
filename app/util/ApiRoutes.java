package util;

import models.Account;
import models.Post;
import models.base.BaseModel;

/**
 * Created by Tobsic on 10.07.2015.
 */
public class ApiRoutes {
    public static String getRoute(Class<?> t, long id) {
        if(!ExposeTools.isExposed(t)) return null;
        if(t == Post.class) return controllers.routes.APIPostController.get(id).toString();
        if(t == Account.class) return controllers.routes.APIUserController.get(id).toString();
        return null;
    }

    public static String getRoute(BaseModel model) {
        return model == null ? null : getRoute(model.getClass(), model.id);
    }

    public static String getRoute(Class<?> t) {
        return getRoute(t, -1);
    }
}
