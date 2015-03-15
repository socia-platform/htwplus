package models.services;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.imgscalr.Scalr;

/**
 * This class handles all avatar aka profile picture related functionalities
 */
public class AvatarService {
    
    private BufferedImage image;
    private File file;
    
    public AvatarService(File file){
        this.file = file;
        try {
            this.image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void pad(){
        //image = Scalr.pad(image, 50);
        //image = Scalr.resize(image, 400);
        //image = Scalr.crop(image, 382, 94, 1860, 1046);
        image = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, 40);
    }
    
    
    public void saveFile() {
        try {
            ImageIO.write(this.image, "jpg", this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
  
}

