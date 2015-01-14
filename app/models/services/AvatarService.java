package models.services;

/**
 * This class handles all avatar aka profile picture related functionalities
 */
public class AvatarService {

    /**
     * Singleton Instance
     */
    private static AvatarService instance;

    public String test;
    private AvatarService(){

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.test = "Yeah Singleton";
    };

    /**
     * Returns the Singleton Instance
     *
     * @return AvatarService
     */
    public static AvatarService getInstance() {
        if(AvatarService.instance == null) {
            AvatarService.instance = new AvatarService();
        }
        return AvatarService.instance;
    };

    public void validateImage(){};

    public void convertImage(){};

    public void createTemp(){};

    public void resizeImage(){};

    public void saveImage(){};

    public void getImage(){};

    public void getTemp(){};
}

