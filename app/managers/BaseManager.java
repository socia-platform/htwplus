package managers;

/**
 * Created by Iven on 17.12.2015.
 */
public interface BaseManager {

    /**
     * Creates this model.
     */
    void create(Object object);

    /**
     * Updates this model.
     */
    void update(Object object);

    /**
     * Deletes this model.
     */
    void delete(Object object);

}
