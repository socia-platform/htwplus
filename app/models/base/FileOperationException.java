package models.base;

/**
 * Created by Fabi on 17.03.2015.
 */
public class FileOperationException extends Exception  {
    public FileOperationException(){}
    public FileOperationException(String message){
        super(message);
    }
}
