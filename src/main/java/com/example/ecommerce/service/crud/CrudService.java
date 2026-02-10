package com.example.ecommerce.service.crud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CrudService<C, U, R, I> {

    R create(C createDto);

    Page<R> findAll(Pageable pageable);

    R findByCode(I id);

    R update(I id, U updateDto);

    void delete(I id);
}
