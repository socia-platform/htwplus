package mock;

import models.base.BaseModel;

import java.util.Random;

/**
 * Empty mock for base model.
 */
public class MockBaseModel extends BaseModel {
    public MockBaseModel() {
        this.id = (long) (new Random()).nextInt(10) -100L; // generate a random ID with a negative number to avoid problems with real models
    }

    @Override
    public void create() {
    }

    @Override
    public void update() {
    }

    @Override
    public void delete() {
    }
}
