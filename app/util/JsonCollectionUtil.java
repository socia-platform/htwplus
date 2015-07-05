package util;

import akka.japi.Pair;
import controllers.BaseController;
import models.base.BaseModel;
import models.enums.CustomContentType;
import net.hamnaberg.json.*;
import net.hamnaberg.json.Error;
import net.hamnaberg.json.parser.CollectionParser;
import play.mvc.Controller;
import play.mvc.Http;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * Get a property that represents the given field
     * @param name Name of the Property
     * @param field Field that should be extracted
     * @return A property that represents the given field
     *
     * if access denied "null" else
     */
    public static Property getProperty(Object obj, String name, Field field) {
        try {
            Object value = field.get(obj);
            return Property.value(name, value instanceof BaseModel ? ((BaseModel) value).id : value);
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
    public static Stream<Property> properties(Object obj, Stream<Pair<String, Field>> fields) {
        return fields
                .map(f -> getProperty(obj, f.first(), f.second()))
                .filter(p -> p != null);
    }

    /**
     * Transform this model to JSON+Collection-Item
     * @param baseUri Uri to the collection of this model
     * @param fields Fields that should be extracted
     * @return A JSON+Collection-Item that represents this model
     */
    public static Item modelToItem(BaseModel model, String baseUri, List<Pair<String, Field>> fields) {
        return Item.create(
                URI.create(baseUri + "/" + model.id),
                properties(model, fields.stream()).collect(Collectors.toList()));
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
        String filter = Controller.request().getQueryString("fields");
        List<Pair<String, Field>> fields = ExposeTools.streamFields(t, filter).collect(Collectors.toList());
        return models
                .map(m -> modelToItem(m, baseUri, fields));
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
        String offset = request.getQueryString("start");
        String limit = request.getQueryString("page-size");

        Pagination pagination = t.isAnnotationPresent(Pagination.class) ?
                t.getAnnotation(Pagination.class) :
                BaseModel.class.getAnnotation(Pagination.class);

        int off = (offset == null || !offset.matches("\\d+") ? 0 : Integer.parseInt(offset));
        int lim = (limit == null || !limit.matches("\\d+") ?
                pagination.pageSize() :
                Math.min(Integer.parseInt(limit), pagination.maxPageSize()));

        return modelsToItems(t, models.apply(off, lim).limit(lim));
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

    public static <T extends BaseModel>
    Collection getCollectionFromItems(Class<T> t, Stream<Item> items) {
        String fields = Controller.request().getQueryString("fields");
        String baseUri = BaseController.getBaseUri().toString();
        URI fullUri = BaseController.getFullUri();
        Map<String, String[]> params = Controller.request().queryString();
        Map<String, String[]> paramsFirst = new HashMap<>(params);
        paramsFirst.remove("start"); // Map<String, String[]> to ?params

        List<Link> links = new ArrayList<>();
        links.add(Link.create(fullUri, "self"));
//		links.add(Link.create(URI.create("href"), "first"));
//		links.add(Link.create(URI.create("href"), "previous"));
//		links.add(Link.create(URI.create("href"), "next"));
//		links.add(Link.create(URI.create("href"), "last"));

        return Collection.create(
                fullUri,
                links,
                items.collect(Collectors.toList()),
                new ArrayList<>(),
                templateFromStream(ExposeTools.streamTemplate(t, fields)),
                Error.EMPTY
        );
    }

    public static <T extends BaseModel>
    Collection getRequestedCollection(Class<T> t, BiFunction<Integer, Integer, List<T>> models) {
        return getCollectionFromItems(t, requestedItemsFromListFunction(t, models));
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
        String fields = Controller.request().getQueryString("fields");
        return Collection.create(
                BaseController.getFullUri(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                templateFromStream(ExposeTools.streamTemplate(t, fields)),
                Error.create(title, code, message)
        );
    }
}
