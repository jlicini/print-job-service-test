package com.adobe.printservice.web;

import com.adobe.printservice.model.RenderTemplate;
import com.adobe.printservice.repository.RenderTemplateRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/templates")
public class RenderTemplateResource {

    private final RenderTemplateRepository renderTemplateRepository;

    public RenderTemplateResource(RenderTemplateRepository renderTemplateRepository) {
        this.renderTemplateRepository = renderTemplateRepository;
    }

    @GetMapping
    public List<RenderTemplate> getTemplates() {
        return renderTemplateRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RenderTemplate> getTemplate(@PathVariable String id) {
        return renderTemplateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
