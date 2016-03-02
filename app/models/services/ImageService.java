package models.services;

import models.base.FileOperationException;
import org.imgscalr.Scalr;
import play.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * This class handles all image related functionalities
 */
public class ImageService {

    final static Logger.ALogger logger = Logger.of(ImageService.class);

    private ImageService(){}
    
    static public boolean validateMinSize(File file, int min_length, int min_height){
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            if(image.getWidth() < min_length) {
                return false;
            }
            if(image.getHeight() < min_height) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    static public boolean validateMaxSize(File file, int max_length, int max_height){
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            if(image.getWidth() > max_length) {
                return false;
            }
            if(image.getHeight() > max_height) {
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
            saveToJPG(image, file);
            image.flush();
        } catch (IOException | IllegalArgumentException e){
            logger.error(e.getMessage(), e);
            throw new FileOperationException("Cropping failed");
        }
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
            saveToJPG(image, file);
            image.flush();
        } catch (IOException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new FileOperationException("Resizing failed");
        }
    }

    static private void saveToJPG(BufferedImage image, File file) throws IOException {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        newImage.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);
        ImageIO.write(newImage, "jpg", file);
        newImage.flush();
    }
    
}

