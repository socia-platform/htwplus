package services;

import com.google.inject.ImplementedBy;

/**
 * Created by Iven on 02.12.2015.
 */
@ImplementedBy(PostgresInit.class)
public interface DatabaseService {

    void initialization();
}
