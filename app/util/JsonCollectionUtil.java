package util;

import models.enums.CustomContentType;
import net.hamnaberg.json.Collection;
import net.hamnaberg.json.parser.CollectionParser;
import play.mvc.Http;

import java.io.IOException;

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

}
