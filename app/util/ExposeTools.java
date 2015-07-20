package util;

import akka.japi.Pair;
import com.google.common.collect.Sets;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by Tobsic on 03.07.2015.
 */
public class ExposeTools {
    public static Stream<Pair<Field,Expose>> streamAllExposedFields(Class t) {
        if (t.getSuperclass() != Object.class)
            return Stream.concat(streamAllExposedFields(t.getSuperclass()), Stream.of(t.getDeclaredFields()).
                    filter(f -> f.isAnnotationPresent(Expose.class)).
                    map(f -> new Pair<>(f, f.getAnnotation(Expose.class))));
        return Stream.of(t.getDeclaredFields()).
                filter(f -> f.isAnnotationPresent(Expose.class)).
                map(f -> new Pair<>(f, f.getAnnotation(Expose.class)));
    }

    public static Stream<Pair<Field,Expose>> streamFieldsExposes(Class<?> t, String fields) {
        if(fields == null) return streamAllExposedFields(t);
        Set<String> filterSet = Sets.newHashSet(fields.split(","));
        return streamAllExposedFields(t).filter(f -> filterSet.contains(f.second().name()));
    }

    public static Stream<Pair<String,Field>> streamFields(Class<?> t, String filter) {
        return streamFieldsExposes(t, filter).map(fe -> new Pair<>(fe.second().name(), fe.first()));
    }

    public static Stream<Pair<String,String>> streamTemplate(Class<?> t, String filter) {
        return streamFieldsExposes(t, filter)
                .map(f -> f.second())
                .filter(e -> !e.template().isEmpty())
                .map(e -> new Pair<>(e.name(), e.template()));
    }

    public static Stream<Pair<String,String>> streamTemplate(Class<?> t) {
        return streamAllExposedFields(t)
                .map(f -> f.second())
                .filter(e -> !e.template().isEmpty())
                .map(e -> new Pair<>(e.name(), e.template()));
    }

    public static Stream<Pair<Field,Expose>> streamFilterableFieldsExposes(Class<?> t) {
        return streamAllExposedFields(t).filter(f -> f.second().filterable());
    }

    public static Stream<Pair<String,Field>> streamFilterableFields(Class<?> t) {
        return streamFilterableFieldsExposes(t).map(fe -> new Pair<>(fe.second().name(), fe.first()));
    }
}
