package com.example.exceptions;

public class CategoryNameFoundException extends RuntimeException {
    public CategoryNameFoundException(String message) {
        super(message);
    }
}
