package com.example.ecommerce.service.support;

import com.example.ecommerce.domain.entity.BaseEntity;
import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.repository.CodeRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Objects;
import java.util.UUID;

public final class CrudEntitySupport {

    private CrudEntitySupport() {
    }

    public static <E extends BaseEntity> E requireByCode(CodeRepository<E> repository, String entityName, UUID code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException(entityName, code));
    }

    public static void requireVersionMatch(BaseEntity entity, Long expectedVersion, Class<?> entityClass) {
        if (!Objects.equals(entity.getVersion(), expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(entityClass, entity.getCode());
        }
    }
}
