package com.adobe.printservice.exception;

public class TemplateNotFoundException extends RuntimeException {

    private static final String TEMPLATE_NOT_FOUND_MESSAGE = "Template does not exist: %s";

    public TemplateNotFoundException(String templateId) {
        super(TEMPLATE_NOT_FOUND_MESSAGE.formatted(templateId));
    }
}
