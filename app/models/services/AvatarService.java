package models.services;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.IOException;

import models.base.FileOperationException;
import play.Logger;

import org.imgscalr.Scalr;

/**
 * This class handles all avatar aka profile picture related functionalities
 */
public class AvatarService {

    final static Logger.ALogger logger = Logger.of(AvatarService.class);
    
    static public int MIN_HEIGHT = 600;
    static public int MIN_WIDTH = 600;
    
    static public int AVATAR_WIDTH = 600;
    static public int AVATAR_HEIGHT = 600;
    static public int THUMB_WIDTH = 140;
    static public int THUMB_HEIGHT = 140;
    
    private AvatarService(){};
    
    static public boolean validateSize(File file){
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            if(image.getWidth() < MIN_WIDTH) {
                return false;
            }
            if(image.getHeight() < MIN_HEIGHT) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    static public void crop(File file, int x, int y, int width, int height) throws FileOperationException {
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            image = Scalr.crop(image, x, y, width, height);
            ImageIO.write(image, "jpg", file);
            image.flush();
        } catch (IOException | IllegalArgumentException e){
            logger.error(e.getMessage(), e);
            throw new FileOperationException("Cropping failed");
        }
    }
    
    static public void resizeToAvatar(File file) throws FileOperationException {
        AvatarService.resize(file, AVATAR_WIDTH, AVATAR_HEIGHT);
    }

    static public void resizeToThumbnail(File file) throws FileOperationException {
        AvatarService.resize(file, THUMB_WIDTH, THUMB_HEIGHT);
    }

    static public void resize(File file, int width, int height) throws FileOperationException {
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            image = Scalr.resize(image, 
                    Scalr.Method.ULTRA_QUALITY, 
                    Scalr.Mode.FIT_EXACT, 
                    width, 
                    height);
            ImageIO.write(image, "jpg", file);
            image.flush();
        } catch (IOException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new FileOperationException("Resizing failed");
        }
    }
    
}

