package invalid.showme.exceptions;

public class StrangeUsageException extends Exception
{
    public StrangeUsageException(String msg) {
        super(msg);
    }

    public StrangeUsageException(String msg, Exception e) {
        super(msg, e);
    }

    public StrangeUsageException() {

    }
}
