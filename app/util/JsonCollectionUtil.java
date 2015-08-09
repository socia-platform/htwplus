package util;

import akka.japi.Pair;
import controllers.BaseController;
import models.base.BaseModel;
import models.enums.CustomContentType;
import net.hamnaberg.funclite.Optional;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Collection;
import net.hamnaberg.json.Error;
import net.hamnaberg.json.parser.CollectionParser;
import play.mvc.Controller;
import play.mvc.Http;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by richard on 11.06.15.
 */
public class JsonCollectionUtil {
    public static final String paramFilterPrefix = "filter_";
    public static final String paramFields = "fields";
    public static final String paramOrderBy = "order_by";
    public static final String paramOffset = "offset";
    public static final String paramLimit = "limit";
    public static final String responseIdPostfix = "_id";
    public enum BaseModelType { none, notExposed, exposed }

    public static BaseModelType getType(Class<?> c) {
        return !BaseModel.class.isAssignableFrom(c) ? BaseModelType.none :
                ExposeTools.isExposed(c) ? BaseModelType.exposed : BaseModelType.notExposed;
    }

    public static BaseModelType getType(Object object) {
        return getType(object.getClass());
    }

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
        List<String> missingData = model.getPropertyNames().stream()
                .filter(s -> !data.containsKey(s))
                .collect(Collectors.toList());
        if (!missingData.isEmpty()) {
            Error error = Error.create("Missing data", "422", "Missing data: " + String.join(", ", missingData));
            validated = Collection.create(col.getHref().get(), col.getLinks(), col.getItems(), col.getQueries(), col.getTemplate().get(), error);
        } else {
            validated = col;
        }
        return validated;
    }

    /**
     * Creates a Template from stream of fields and template values
     * @param properties Pairs of property names and property values
     * @return A JSON+Collection-Template
     */
    public static Template templateFromStream(Stream<Pair<String,String>> properties) {
        return Template.create(properties
                .map(p -> Property.value(p.first(), p.second()))
                .collect(Collectors.toList()));
    }

    public static Object getValue(Object object, Field field) {
        try {
            return field.get(object);
        } catch(IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a property that represents the given field
     * @param name Name of the Property
     * @param field Field that should be extracted
     * @return A property that represents the given field
     *
     * if access denied "null" else
     */
    public static Property getProperty(Object obj, String name, Field field, String baseUri) {
        try {
            Object value = field.get(obj);
            if(value instanceof BaseModel)
                return Property.value(name, baseUri + ApiRoutes.getRoute((BaseModel)value)); // TODO: Expand?
            else
                return Property.value(name, value); // TODO: Expand?
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a stream of properties that represents the given fields
     * @param fields Fields that should be extracted
     * @return A list of properties that represents the given fields (not accessible files are not included)
     */
    public static Stream<Property> properties(Object obj, Stream<Pair<String, Field>> fields, String baseUri) {
        return fields
                .map(f -> getProperty(obj, f.first(), f.second(), baseUri))
                .filter(Objects::nonNull);
    }

    public static Long getId(BaseModel model) {
        return model == null ? null : model.id;
    }

    public static URI getUri(BaseModel model, String baseUri) {
        String href = ApiRoutes.getRoute(model);
        return href == null ? null : URI.create(baseUri + href);
    }

    public static Link getLink(BaseModel model, String rel, String baseUri) {
        URI uri = getUri(model, baseUri);
        return uri == null ? null : Link.create(uri, rel);
    }

    /**
     * Transform this model to JSON+Collection-Item
     * @param baseUri Uri to the collection of this model
     * @param fields Fields that should be extracted
     * @return A JSON+Collection-Item that represents this model
     */
    public static Item modelToItem(BaseModel model, String baseUri, Map<BaseModelType,List<Pair<String,Field>>> fields) {
        String route = ApiRoutes.getRoute(model);
        if(route == null) {
            System.err.println("JSON-Api: Tried to publish property of type with no @ExposeClass annotation");
            return null;
        }
        List<Link> links = fields.getOrDefault(BaseModelType.exposed, new LinkedList<>()).stream()
                .map(f -> getLink((BaseModel) getValue(model, f.second()), f.first(), baseUri))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Property> properties = Stream.concat(Stream.concat(
                        fields.getOrDefault(BaseModelType.notExposed, new LinkedList<>()).stream()
                                .map(f -> Property.value(f.first() + responseIdPostfix, getId((BaseModel) getValue(model, f.second())))),
                        fields.getOrDefault(BaseModelType.none, new LinkedList<>()).stream()
                                .map(f -> Property.value(f.first(), getValue(model, f.second())))),
                        fields.getOrDefault(BaseModelType.exposed, new LinkedList<>()).stream()
                                .map(f -> Property.value(f.first() + "_id", getId((BaseModel)getValue(model, f.second())))))
                        .collect(Collectors.toList());
        return Item.create(URI.create(baseUri + route), properties, links);
//        return Item.create(URI.create(route), properties(model, fields.stream()).collect(Collectors.toList()));
    }

    /**
     * Transform all given models to JSON+Collection-Items
     * @param t Class of the model that should be transformed
     * @param models A stream of models that should be transformed
     * @param <T> Type of the model that should be transformed
     * @return A stream of JSON+Collection-Items which represents the models
     */
    public static <T extends BaseModel>
    Stream<Item> modelsToItems(Class<T> t, Stream<T> models) {
        if(models == null) return null;
        String baseUri = BaseController.getBaseUri().toString();
        String filter = Controller.request().getQueryString(paramFields);
        Map<BaseModelType,List<Pair<String,Field>>> fields = ExposeTools.streamFields(t, filter)
                .collect(Collectors.groupingBy(f -> getType(f.second().getType())));

        return models
                .map(m -> modelToItem(m, baseUri, fields))
                .filter(Objects::nonNull);
    }

    /**
     * Get a stream of JSON+Collection-Items, that contain the the exposed fields which are requested. Fields, offset
     * and limit is extracted from request
     * @param t Class of items
     * @param models Function, that give a chunk (offset, limit) of models to transform
     * @param <T> Type of models to transform
     * @return A list of items that represent the requested models
     */
    public static <T extends BaseModel>
    Stream<Item> requestedItemsFromStreamFunction(Class<T> t, BiFunction<Integer, Integer, Stream<T>> models) {
        Http.Request request = Controller.request();
        String offset = request.getQueryString(paramOffset);
        String limit = request.getQueryString(paramLimit);

        ExposeClass exposeClass = t.isAnnotationPresent(ExposeClass.class) ?
                t.getAnnotation(ExposeClass.class) :
                BaseModel.class.getAnnotation(ExposeClass.class);

        int off = (offset == null || !offset.matches("\\d+") ? 0 : Integer.parseInt(offset));
        int lim = (limit == null || !limit.matches("\\d+") ?
                exposeClass.defaultLimit() :
                Math.min(Integer.parseInt(limit), exposeClass.maxLimit()));

        return modelsToItems(t, models.apply(off, lim).limit(lim));
    }

    /**
     * Get a stream of JSON+Collection-Items, that contain the the exposed fields which are requested. Fields, offset
     * and limit is extracted from request
     * @param t Class of items
     * @param models Function, that give a chunk (offset, limit, filter, order) of models to transform
     * @param <T> Type of models to transform
     * @return A list of items that represent the requested models
     */
    public static <T extends BaseModel>
    Stream<Item> filteredRequestedItemsFromListFunction(Class<T> t, QuinFunction<Class<T>, Integer, Integer,
            Map<String,String[]>, String, List<T>> models) {
        Map<String,String> nameMap = ExposeTools.filterableNamesToFieldNames(t, paramFilterPrefix);
        String order = Controller.request().getQueryString(paramOrderBy);
        Map<String,String[]> filter = Controller.request().queryString().entrySet().stream()
                .filter(e -> nameMap.containsKey(e.getKey()))
                .collect(Collectors.toMap(e -> nameMap.get(e.getKey()), Map.Entry::getValue));
        return requestedItemsFromStreamFunction(t, (off, lim) -> models.apply(t, off, lim, filter, order).stream());
    }

    /**
     * Get a stream of JSON+Collection-Items, that contain the the exposed fields which are requested. Fields, offset
     * and limit is extracted from request
     * @param t Class of items
     * @param models Function, that give a chunk (offset, limit) of models to transform
     * @param <T> Type of models to transform
     * @return A list of items that represent the requested models
     */
    public static <T extends BaseModel>
    Stream<Item> requestedItemsFromListFunction(Class<T> t, BiFunction<Integer, Integer, List<T>> models) {
        return requestedItemsFromStreamFunction(t, (Integer off, Integer lim) -> models.apply(off, lim).stream());
    }

    /**
     * Get a stream of JSON+Collection-Items, that contain the the exposed fields which are requested. Fields, offset,
     *  limit and filter is extracted from request
     * @param t Class of items
     * @param models Function, that give a chunk (offset, limit, filter) of models to transform
     * @param <T> Type of models to transform
     * @return A list of items that represent the requested models
     */
    public static <T extends BaseModel>
    Stream<Item> requestedItemsFromListFunction(Class<T> t, QuinFunction<Class<T>, Integer, Integer, Map<String,String[]>, String, List<T>> models) {
        Map<String,String[]> filter = new HashMap<>();
        String order = Controller.request().getQueryString(paramOrderBy);
        return requestedItemsFromStreamFunction(t,
                (Integer off, Integer lim) -> models.apply(t, off, lim, filter, order).stream());
    }

    /**
     * Get a stream of JSON+Collection-Items, that contain the the exposed fields which are requested. Fields, offset
     * and limit is extracted from request
     * @param t Class of items
     * @param models A list of models that should be transformed
     * @param <T> Type of models to transform
     * @return A list of items that represent the requested models
     */
    public static <T extends BaseModel>
    Stream<Item> requestedItems(Class<T> t, Stream<T> models) {
        return requestedItemsFromStreamFunction(t, (Integer off, Integer lim) -> models.skip(off).limit(lim));
    }

    public static Query createQuery(URI baseUri, String rel, Map<String,String[]> params, Map<String,Set<String>> filter) {
        return Query.create(
                baseUri,
                rel,
                Optional.<String>none(),
                params.entrySet().stream()
                        .filter(e -> !filter.containsKey(e.getKey()) || filter.get(e.getKey()) != null)
                        .flatMap(e -> Arrays.stream(e.getValue())
                                .filter(v -> !filter.containsKey(e.getKey()) || !filter.get(e.getKey()).contains(v))
                                .map(v -> Property.value(e.getKey(), v)))
                        .collect(Collectors.toList()));
    }

    public static <T extends BaseModel>
    Collection getCollectionFromItems(Class<T> t, Stream<Item> items) {
        URI baseUri = BaseController.getBaseUri();
        URI fullUri = BaseController.getFullUri();
        Map<String, String[]> params = Controller.request().queryString();
        Map<String, String[]> paramsFirst = new HashMap<>(params);
        paramsFirst.remove(paramOffset); // Map<String, String[]> to ?params

        List<Link> links = new ArrayList<>();
        links.add(Link.create(fullUri, "self"));

        List<Query> queries = new ArrayList<>();
        Map<String,Set<String>> firstMap = new HashMap<>();
        firstMap.put(paramOffset, null);
        queries.add(createQuery(baseUri, "first", params, firstMap));
//		links.add(Link.create(URI.create("href"), "first"));
//		links.add(Link.create(URI.create("href"), "previous"));
//		links.add(Link.create(URI.create("href"), "next"));
//		links.add(Link.create(URI.create("href"), "last"));

        return Collection.create(
                fullUri,
                links,
                items.collect(Collectors.toList()),
                queries,
                templateFromStream(ExposeTools.streamTemplate(t)),
                Error.EMPTY
        );
    }

    public static <T extends BaseModel>
    Collection getRequestedCollection(Class<T> t, BiFunction<Integer, Integer, List<T>> models) {
        return getCollectionFromItems(t, requestedItemsFromListFunction(t, models));
    }

    public static <T extends BaseModel>
    Collection getRequestedCollection(Class<T> t, QuinFunction<Class<T>, Integer, Integer, Map<String,String[]>, String, List<T>> models) {
        return getCollectionFromItems(t, filteredRequestedItemsFromListFunction(t, models));
    }

    public static <T extends BaseModel>
    Collection getCollection(Class<T> t, Stream<T> models) {
        return getCollectionFromItems(t, modelsToItems(t, models));
    }

    public static <T extends BaseModel>
    Collection getRequestedCollection(Class<T> t, List<T> models) {
        return getCollection(t, models.stream());
    }

    public static <T extends BaseModel>
    Collection getErrorCollection(Class<T> t, String title, String code, String message) {
        String fields = Controller.request().getQueryString(paramFields);
        return Collection.create(
                BaseController.getFullUri(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                templateFromStream(ExposeTools.streamTemplate(t)),
                Error.create(title, code, message)
        );
    }

    public static <T extends BaseModel> Collection addTemplate(Class<T> t, Collection collection) {
        return Collection.create(
                collection.getHref().get(),
                collection.getLinks(),
                collection.getItems(),
                collection.getQueries(),
                getTemplate(t),
                collection.getError().get()
        );
    }

    public static <T extends BaseModel> Template getTemplate(Class<T> t) {
        return templateFromStream(ExposeTools.streamTemplate(t));
    }
}
