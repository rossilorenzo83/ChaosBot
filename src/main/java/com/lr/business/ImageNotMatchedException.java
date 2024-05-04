package com.lr.business;

import lombok.Getter;

@Getter
public class ImageNotMatchedException extends Exception {

    private Boolean inMainMap;

    public ImageNotMatchedException(String errorMessage, Boolean inMainMap) {
        super(errorMessage);
        this.inMainMap = inMainMap;
    }
}
