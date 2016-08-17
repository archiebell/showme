package invalid.showme.exceptions;

public class ServerException extends Exception
{
    public ServerException(String msg) {
        super(msg);
    }

    public ServerException(String errMsg, Exception e) {
        super(errMsg, e);
    }
}
