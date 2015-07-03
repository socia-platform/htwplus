package models.base;

import com.sun.xml.internal.bind.v2.TODO;
import controllers.BaseController;
import net.hamnaberg.json.Item;
import net.hamnaberg.json.Property;
import play.mvc.Controller;
import play.mvc.Http.Request;
import util.Expose;
import util.ExposeTools;
import util.Pagination;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.*;

/**
 * Provides a base model with standard attributes like ID, creation/modification date for all persistence models.
 */
@MappedSuperclass
@Pagination
public abstract class BaseModel {
    /**
     * Model ID.
     */
	@Id
	@GeneratedValue
	public Long id;

    /**
     * Date of the creation time of this model.
     */
	@Column(name = "created_at")
	public Date createdAt;

    /**
     * Date of the last modification of this model.
     */
	@Column(name = "updated_at")
	public Date updatedAt;

    /**
     * Sets created and updated time on creation.
     */
	@PrePersist
	protected void createdAt() {
		this.createdAt = this.updatedAt = new Date();
	}

    /**
     * Sets last update time.
     */
	@PreUpdate
	protected void updatedAt() {
		this.updatedAt = new Date();
	}

    /**
     * Creates this model.
     */
	public abstract void create();

    /**
     * Updates this model.
     */
	public abstract void update();

    /**
     * Deletes this model.
     */
	public abstract void delete();

    /**
     * Returns true, if given Object obj is equal to this model.
     *
     * @param obj Object instance to check equality
     * @return True, of given Object obj is equal this
     */
    @Override
	public boolean equals(Object obj) {
		return obj instanceof BaseModel
                && ((BaseModel)obj).id != null
                && ((BaseModel)obj).id.equals(this.id);
	}

	/**
	 * Get a property that represents the given field
	 * @param field Field that should be extracted
	 * @return A property that represents the given field
	 *
	 * if access denied "null" else
	 * {
	 * 	   name: if exposed "expose.name" else "field.name"
	 * 	   value: if is BaseModel "BaseModel.id" else "field.value"
	 * }
	 */
	public Property getProperty(Field field) {
		try {
			Object value = field.get(this);
			return Property.value(
					field.isAnnotationPresent(Expose.class) ?
							field.getAnnotation(Expose.class).name() :
							field.getName(),
					value instanceof BaseModel ?
							((BaseModel)value).id :
							value
			);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get a list of properties that represent the given fields
	 * @param fields Fields that should be extracted
	 * @return A list of properties that represents the given fields (not accessible files are not included)
	 */
	public List<Property> getProperties(List<Field> fields) {
		return fields.stream()
				.map(this::getProperty)
				.filter(p -> p != null)
				.collect(Collectors.toList());
	}

	/**
	 * Transform this model to JSON+Collection-Item
	 * @param baseUri Uri to the collection of this model
	 * @param fields Fields that should be extracted
	 * @return A JSON+Collection-Item that represents this model
	 */
	public Item toItem(String baseUri, List<Field> fields) {
		return Item.create(URI.create(baseUri + "/" + this.id), this.getProperties(fields));
	}

	/**
	 * Get a list of requested (and of course exposed) fields as JSON+Collection-Item
	 * @return A list of requested (and of course exposed) fields as JSON+Collection-Item
	 */
	public List<Property> getProperties() {
		return getProperties(ExposeTools.getRequestedFields(this.getClass()));
	}

	/**
	 * Get a list of JSON+Collection-Items, that contain the the exposed fields which are requested. Filter, offset and
	 * limit is extracted from request
	 * @param t Class of items
	 * @param models Function, that give a chunk (offset, limit) of models to transform
	 * @param <T> Type of models to transform
	 * @return A list of items that represent the requested models
	 */
	public static <T extends BaseModel> List<Item> getRequestedItems(Class<T> t, BiFunction<Integer, Integer, List<T>> models) {
		Request request = Controller.request();
		String offset = request.getQueryString("start");
		String limit = request.getQueryString("page-size");

		Pagination pagination = t.isAnnotationPresent(Pagination.class) ?
				t.getAnnotation(Pagination.class) :
				BaseModel.class.getAnnotation(Pagination.class);

		int off = (offset == null || !offset.matches("\\d+") ? 0 : Integer.parseInt(offset));
		int lim = (limit == null || !limit.matches("\\d+") ?
				pagination.pageSize() :
				Math.min(Integer.parseInt(limit), pagination.maxPageSize()));

		return toItems(t, models.apply(off, lim).stream().limit(lim));
	}

	/**
	 * Get a list of JSON+Collection-Items, that contain the the exposed fields which are requested. Filter, offset and
	 * limit is extracted from request
	 * @param t Class of items
	 * @param models A list of models that should be transformed
	 * @param <T> Type of models to transform
	 * @return A list of items that represent the requested models
	 */
	public static <T extends BaseModel> List<Item> getRequestedItems(Class<T> t, List<T> models) {
		Request request = Controller.request();
		String offset = request.getQueryString("start");
		String limit = request.getQueryString("page-size");

		Pagination pagination = t.getAnnotation(Pagination.class);
		int off = (offset == null || !offset.matches("\\d+") ? 0 : Integer.parseInt(offset));
		int lim = (limit == null || !limit.matches("\\d+")) ?
				pagination.pageSize() :
				Math.min(Integer.parseInt(limit), pagination.maxPageSize());

		return toItems(t, models.stream().skip(off).limit(lim));
	}

	/**
	 * Transform all given models to JSON+Collection-Items
	 * @param t Class of the model that should be transformed
	 * @param models A list of models that should be transformed
	 * @param <T> Type of the model that should be transformed
	 * @return A list of JSON+Collection-Items which represents the models
	 */
	public static <T extends BaseModel> List<Item> toItems(Class<T> t, Stream<T> models) {
		// TODO: add links
		if(models == null) return null;
		String baseUri = BaseController.getBaseUri().toString();
		List<Field> fields = ExposeTools.getRequestedFields(t);
		return models
				.map(m -> m.toItem(baseUri, fields))
				.collect(Collectors.toList());
	}


	public static List<String> getPropertyNames(BaseModel model) {
		List<String> nameList = new ArrayList<>();
		Field[] fields = model.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.getAnnotation(Expose.class) != null) {
				nameList.add(f.getAnnotation(Expose.class).name());
			}
		}
		return nameList;
	}
}
