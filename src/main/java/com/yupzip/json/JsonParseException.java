package com.yupzip.json;

public class JsonParseException extends RuntimeException {

    public JsonParseException(Throwable throwable) {
        super(throwable);
    }

    public JsonParseException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
