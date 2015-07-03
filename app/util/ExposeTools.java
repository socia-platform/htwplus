package util;

import com.google.common.collect.Sets;
import play.mvc.Controller;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Tobsic on 03.07.2015.
 */
public class ExposeTools {
    public static Stream<Field> streamExposedFields(Class t){
        return Stream.of(t.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Expose.class));
    }

    public static List<Field> getExposedFields(Class t){
        return streamExposedFields(t).collect(Collectors.toList());
    }

    public static Stream<Field> streamRequestedFields(Class<?> t) {
        String filter = Controller.request().getQueryString("fields");
        if(filter == null) return streamExposedFields(t);
        Set<String> filterSet = Sets.newHashSet(filter.split(","));
        return streamExposedFields(t).filter(f -> filterSet.contains(f.getName()));
    }

    public static List<Field> getRequestedFields(Class<?> t) {
        return streamRequestedFields(t).collect(Collectors.toList());
    }
}
