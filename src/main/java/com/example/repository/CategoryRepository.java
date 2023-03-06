package com.example.repository;

import com.example.entity.CategoryEntity;
import com.example.entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends CrudRepository<CategoryEntity, Long> {
    @Query("from CategoryEntity")
    List<CategoryEntity> findAll();

    Optional<CategoryEntity> findByNameUz(String nameUz);
    Optional<CategoryEntity> findByNameRu(String nameRu);

    @Query("from CategoryEntity where nameUz=?1 or nameRu=?1")
    Optional<CategoryEntity> findByName(String name);
}
