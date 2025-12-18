package tw.com.ispan.eeit.ho_back.common.exception;

import java.util.Map;

public class LoginException extends RuntimeException {
    private Map<String, String> errors;

    public LoginException(Map<String, String> errors) {
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}