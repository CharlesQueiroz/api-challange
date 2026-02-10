package com.example.ecommerce.repository;

import com.example.ecommerce.domain.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface CodeRepository<T extends BaseEntity> extends JpaRepository<T, Long> {

    Optional<T> findByCode(UUID code);
}
