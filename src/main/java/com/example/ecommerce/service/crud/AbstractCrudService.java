package com.example.ecommerce.service.crud;

import com.example.ecommerce.domain.entity.BaseEntity;
import com.example.ecommerce.dto.common.VersionedUpdateDTO;
import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.repository.CodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Transactional(readOnly = true)
public abstract class AbstractCrudService<E extends BaseEntity, C, U extends VersionedUpdateDTO, R> implements CrudService<C, U, R, UUID> {

    protected abstract CodeRepository<E> repository();

    protected abstract String entityName();

    protected abstract Class<E> entityClass();

    protected abstract E toEntity(C createDto);

    protected abstract void updateEntity(U updateDto, E entity);

    protected abstract R toResponse(E entity);

    @Override
    @Transactional
    public R create(C createDto) {
        var entity = toEntity(createDto);
        var saved = repository().save(entity);
        return toResponse(saved);
    }

    @Override
    public Page<R> findAll(Pageable pageable) {
        return repository().findAll(pageable).map(this::toResponse);
    }

    @Override
    public R findByCode(UUID code) {
        return toResponse(findEntityByCode(code));
    }

    @Override
    @Transactional
    public R update(UUID code, U updateDto) {
        var entity = findEntityByCode(code);
        if (!Objects.equals(entity.getVersion(), updateDto.version())) {
            throw new ObjectOptimisticLockingFailureException(entityClass(), entity.getCode());
        }

        updateEntity(updateDto, entity);
        var saved = repository().saveAndFlush(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID code) {
        var entity = findEntityByCode(code);
        beforeDelete(entity);
        repository().delete(entity);
        afterDelete(entity);
    }

    protected E findEntityByCode(UUID code) {
        return repository().findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException(entityName(), code));
    }

    protected void beforeDelete(E entity) {
    }

    protected void afterDelete(E entity) {
    }
}
