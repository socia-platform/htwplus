package util;

import akka.japi.Pair;
import com.google.common.collect.Sets;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Tobsic on 03.07.2015.
 */
public class ExposeTools {
    public static boolean isExposed(Field field) {
        return field.isAnnotationPresent(ExposeField.class);
    }

    public static boolean isExposed(Class<?> t) {
        return t.isAnnotationPresent(ExposeClass.class);
    }

    public static String getName(Pair<Field, ExposeField> fe) {
        return fe.second().name().isEmpty() ? fe.first().getName() : fe.second().name();
    }

    public static Stream<Pair<Field, ExposeField>> streamAllExposedFields(Class t) {
        return Stream.of(t.getFields()).
                filter(ExposeTools::isExposed).
                map(f -> new Pair<>(f, f.getAnnotation(ExposeField.class)));
    }

    public static Stream<Pair<Field, ExposeField>> streamFieldsExposes(Class t, String fields) {
        if(fields == null) return streamAllExposedFields(t);
        Set<String> filterSet = Sets.newHashSet(fields.split(","));
        return streamAllExposedFields(t).filter(fe -> filterSet.contains(getName(fe)));
    }

    public static Stream<Pair<String,Field>> streamFields(Class<?> t, String filter) {
        return streamFieldsExposes(t, filter).map(fe -> new Pair<>(getName(fe), fe.first()));
    }

    public static Stream<Pair<String,String>> streamTemplate(Class<?> t) {
        return streamAllExposedFields(t)
                .filter(e -> !e.second().template().isEmpty())
                .map(fe -> new Pair<>(getName(fe),  fe.second().template()));
    }

    public static Stream<Pair<Field, ExposeField>> streamFilterableFieldsExposes(Class<?> t) {
        return streamAllExposedFields(t).filter(f -> f.second().filterable());
    }

    public static Stream<Pair<String,Field>> streamFilterableFields(Class<?> t) {
        return streamFilterableFieldsExposes(t).map(fe -> new Pair<>(getName(fe), fe.first()));
    }

    public static Map<String,String> exposeNamesToFieldNames(Class<?> t) {
        return streamAllExposedFields(t)
                .collect(Collectors.toMap(
                        ExposeTools::getName,
                        fe -> fe.first().getName()));
    }

    public static Map<String,String> filterableNamesToFieldNames(Class<?> t, String prefix) {
        return streamFilterableFieldsExposes(t)
                .collect(Collectors.toMap(
                        fe -> prefix + getName(fe),
                        fe -> fe.first().getName()));
    }
}
