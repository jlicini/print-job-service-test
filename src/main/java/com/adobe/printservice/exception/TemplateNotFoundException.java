package com.adobe.printservice.exception;

public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException(String templateId) {
        super("Template does not exist: " + templateId);
    }
}
