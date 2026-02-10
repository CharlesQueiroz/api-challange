package com.example.ecommerce.mapper;

public interface CrudMapper<E, C, U, R> {

    R toResponseDTO(E entity);

    E toEntity(C createDto);

    void updateEntityFromDTO(U updateDto, E entity);
}
