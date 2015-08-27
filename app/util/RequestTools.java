package util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by richard on 26.08.15.
 */
public class RequestTools {

    public static LinkedList<String> getNull(List<String> params, Map<String, String> request) {
        LinkedList<String> res = new LinkedList<>();
        for (String s : params) {
            if (request.get(s) == null) {
                res.add(s);
            }
        }
        return res;
    }

    public static LinkedList<String> getNull(List<String> params, Set<String> set) {
        LinkedList<String> res = new LinkedList<>();
        for (String s : params) {
            if (!set.contains(s)) {
                res.add(s);
            }
        }
        return res;
    }
}
