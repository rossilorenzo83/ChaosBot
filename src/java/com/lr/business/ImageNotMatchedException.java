package com.lr.business;

public class ImageNotMatchedException extends Exception {
    public ImageNotMatchedException(String errorMessage) {
        super(errorMessage);
    }
}
