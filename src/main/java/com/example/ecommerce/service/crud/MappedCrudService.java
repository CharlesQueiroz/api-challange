package com.example.ecommerce.service.crud;

import com.example.ecommerce.domain.entity.BaseEntity;
import com.example.ecommerce.dto.common.VersionedUpdateDTO;
import com.example.ecommerce.mapper.CrudMapper;
import com.example.ecommerce.repository.CodeRepository;

public abstract class MappedCrudService<E extends BaseEntity, C, U extends VersionedUpdateDTO, R> extends AbstractCrudService<E, C, U, R> {

    private final String entityName;
    private final Class<E> entityClass;
    private final CodeRepository<E> repository;
    private final CrudMapper<E, C, U, R> mapper;

    protected MappedCrudService(CodeRepository<E> repository, CrudMapper<E, C, U, R> mapper, Class<E> entityClass, String entityName) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.entityName = entityName;
    }

    @Override
    protected CodeRepository<E> repository() {
        return repository;
    }

    @Override
    protected String entityName() {
        return entityName;
    }

    @Override
    protected Class<E> entityClass() {
        return entityClass;
    }

    @Override
    protected E toEntity(C createDto) {
        return mapper.toEntity(createDto);
    }

    @Override
    protected void updateEntity(U updateDto, E entity) {
        mapper.updateEntityFromDTO(updateDto, entity);
    }

    @Override
    protected R toResponse(E entity) {
        return mapper.toResponseDTO(entity);
    }

    protected CrudMapper<E, C, U, R> mapper() {
        return mapper;
    }
}
