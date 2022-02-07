package com.backend.qcmplus.utils;

public class ObjectNotFoundException extends Exception {
    public ObjectNotFoundException(Long id) {
        super("Object : " + id + " Not found");
    }
}