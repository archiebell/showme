package invalid.showme.exceptions;

public class InvalidMessageException extends Exception {
    public InvalidMessageException() {}

    public InvalidMessageException(String s) {
        super(s);
    }
}
