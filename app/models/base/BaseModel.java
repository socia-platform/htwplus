package models.base;

import play.db.jpa.JPA;
import util.ExposeField;
import util.ExposeClass;

import java.lang.reflect.Field;
import java.util.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

/**
 * Provides a base model with standard attributes like ID, creation/modification date for all persistence models.
 */
@MappedSuperclass
@ExposeClass
public abstract class BaseModel {
    /**
     * Model ID.
     */
	@Id
	@GeneratedValue
	@ExposeField(name = "id", template = "8")
	public Long id;

    /**
     * Date of the creation time of this model.
     */
	@Column(name = "created_at")
	@ExposeField(name = "created_at")
	public Date createdAt;

    /**
     * Date of the last modification of this model.
     */
	@Column(name = "updated_at")
	@ExposeField(name = "updated_at")
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

	public List<String> getPropertyNames() {
		List<String> nameList = new ArrayList<>();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.getAnnotation(ExposeField.class) != null) {
				nameList.add(f.getAnnotation(ExposeField.class).name());
			}
		}
		return nameList;
	}

	/**
	 * Get one model of type T with a specific ID (null if it's not exists)
	 * @param t Class of the models that should be returned
	 * @param id ID of model, that should be returned
	 * @param <T> Type of the model
	 * @return One model of type T with ID id or null if no model with this id exists
	 */
	public static <T extends BaseModel>
	List<T> one(Class<T> t, long id) {
		if(id < 0 || !t.isAnnotationPresent(Entity.class))
			return null;
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(t);
		List<T> res = JPA.em().createQuery(cq.where(cb.equal(cq.from(t).get("id"), id))).getResultList();
		if(res.size() == 0)
			return null;
		return res;
	}

	/**
	 * Get the JPA-Path to the value of a property or the id, if it is a model as well (needed for filtering)
	 * @param elem JPA-Path to the property
	 * @param <T> Generic-Type of the Path
	 * @return JPA-Path to the property or to the id of the model
	 */
	public static <T>
	Path<T> typeCheck(Path<T> elem) {
		return BaseModel.class.isAssignableFrom(elem.getJavaType()) ? elem.get("id") : elem;
	}

	// TODO: For access specific criteria there may not be a "or" relation, so is it better to introduce a new parameter?
	/**
	 * Get a set of models of type T (e.g. posts, users). This models are paginated (offset, limit) and can be filtered
	 * (map of keys and one or more values), ordered. All filter or order properties has previously checked to be
	 * exposed.
	 * @param t Class of Models to get from JPA
	 * @param offset How many entries to skip
	 * @param limit How many entries to response
	 * @param criteria Filter criteria (pair of key and one or more values). This criteria should contain
	 *                 access-specific criteria.
	 * @param order Property to filter by
	 * @param <T> Type of Models to get from JPA (Type of t)
	 * @return A list of all matching entries
	 */
	public static <T>
	List<T> someWhere(Class<T> t, Integer offset, Integer limit, Map<String, String[]> criteria, String order) {
		if(!t.isAnnotationPresent(Entity.class)) // Break if requested model is not an JPA-Entry
			return null;
		if(order == null || order.isEmpty()) // If no order is given, default to order by id
			order = "id";

		// Prepare the criteria query
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(t);
		Root<T> root = cq.from(t);
		Predicate[] p = criteria.entrySet().stream()
				.filter(c -> c.getValue() != null) // Skip conditions with no values
				.map(c -> typeCheck(root.get(c.getKey())).as(String.class).in(c.getValue())) // Apply criteria (property value is in list of possible values)
				.toArray(Predicate[]::new);

		return JPA.em().createQuery(cq.where(cb.and(p))
				.orderBy(cb.asc(root.get(order))))
				.setFirstResult(offset)
				.setMaxResults(limit)
				.getResultList();
	}
}