package models.base;

import util.Expose;
import util.Pagination;

import java.lang.reflect.Field;
import java.util.*;

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

	public List<String> getPropertyNames() {
		List<String> nameList = new ArrayList<>();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.getAnnotation(Expose.class) != null) {
				nameList.add(f.getAnnotation(Expose.class).name());
			}
		}
		return nameList;
	}
}
