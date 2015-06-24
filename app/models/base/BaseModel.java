package models.base;

import net.hamnaberg.json.Property;
import net.hamnaberg.json.Template;
import net.hamnaberg.json.Value;
import util.Expose;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.*;

/**
 * Provides a base model with standard attributes like ID, creation/modification date for all persistence models.
 */
@MappedSuperclass
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
		return obj.getClass() != BaseModel.class
                && ((BaseModel)obj).id != null
                && ((BaseModel)obj).id.equals(this.id);
	}

	public List<Property> getProperies() {
		List<Property> propList = new ArrayList<Property>();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field f : fields) {
			if (f.getAnnotation(Expose.class) != null) {
				try {
					propList.add(Property.value(f.getName(), f.get(this)));

				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return propList;
	}
}
