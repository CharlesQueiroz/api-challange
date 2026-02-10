package com.example.ecommerce.controller;

import com.example.ecommerce.dto.common.HasCode;
import com.example.ecommerce.service.crud.CrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

import static org.springframework.http.ResponseEntity.created;

public abstract class AbstractCrudController<C, U, R extends HasCode> {

    protected abstract CrudService<C, U, R, UUID> service();

    protected ResponseEntity<R> createResource(C createDto) {
        var response = service().create(createDto);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(response.code())
                .toUri();

        return created(location).body(response);
    }

    protected Page<R> findAllResources(Pageable pageable) {
        return service().findAll(pageable);
    }

    protected R findResourceByCode(UUID code) {
        return service().findByCode(code);
    }

    protected R updateResource(UUID code, U updateDto) {
        return service().update(code, updateDto);
    }

    protected void deleteResource(UUID code) {
        service().delete(code);
    }
}
