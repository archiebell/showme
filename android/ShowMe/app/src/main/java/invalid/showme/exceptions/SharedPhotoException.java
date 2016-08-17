package invalid.showme.exceptions;

public class SharedPhotoException extends Exception
{
    public SharedPhotoException(String msg){
        super(msg);
    }

    public SharedPhotoException(String msg, Exception e) {
        super(msg, e);
    }
}
