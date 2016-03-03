package models;

import play.data.validation.Constraints;

public class Avatar {

    @Constraints.Required
    public Integer x;

    @Constraints.Required
    public Integer y;

    @Constraints.Required
    public Integer width;

    @Constraints.Required
    public Integer height;

    public String validate() {
        if (!this.width.equals(this.height)) {
            return "The chosen extract is not rectangular";
        }
        return null;
    }

}
