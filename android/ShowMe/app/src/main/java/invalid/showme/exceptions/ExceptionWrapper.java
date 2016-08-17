package invalid.showme.exceptions;

public class ExceptionWrapper extends Exception {
    public ExceptionWrapper(String msg, Exception e) {
        super(msg, e);
    }
}
