package com.example.lostfound.service;

public class ImageUploadFailedException extends RuntimeException {

    public ImageUploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
