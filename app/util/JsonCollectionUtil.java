package util;

import models.base.BaseModel;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Error;
import net.hamnaberg.json.parser.CollectionParser;
import play.mvc.Http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by richard on 11.06.15.
 */
public class JsonCollectionUtil {

    public static boolean hasJsonCollection(Http.Request request) {
        return request.getHeader("Content-Type").contains(CustomContentType.JSON_COLLECTION.getIdentifier());
    }

    public static Collection getJsonCollection(Http.Request request) {
        Collection res = null;
        CollectionParser parser = new CollectionParser();
        String str = new String(request.body().asRaw().asBytes());
        try {
            res = parser.parse(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Collection checkForMissingItems(Collection col, BaseModel model) {
        Collection validated;
        Map<String, Property> data = col.getFirstItem().get().getDataAsMap();
        List<String> missingData = new ArrayList<String>();
        for (String s : model.getPropertyNames(model)) {
            if (!data.containsKey(s)) {
                missingData.add(s);
            }
        }
        if (!missingData.isEmpty()) {
            Error error = Error.create("Missing data", "422", "Missing data: " + String.join(", ", missingData));
            validated = Collection.create(col.getHref().get(), col.getLinks(), col.getItems(), col.getQueries(), col.getTemplate().get(), error);
        } else {
            validated = col;
        }
        return validated;
    }

}
