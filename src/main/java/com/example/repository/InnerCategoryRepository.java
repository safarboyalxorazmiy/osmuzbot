package com.example.repository;

import com.example.entity.CategoryEntity;
import com.example.entity.InnerCategoryEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InnerCategoryRepository extends CrudRepository<InnerCategoryEntity, Long> {
    @Query("from CategoryEntity")
    List<InnerCategoryEntity> findAll();

    Optional<InnerCategoryEntity> findByNameUz(String nameUz);
    Optional<InnerCategoryEntity> findByNameRu(String nameRu);

    List<InnerCategoryEntity> findByCategoryId(Long categoryId);

    @Query("from InnerCategoryEntity where (nameUz=?1 or nameRu=?1) and (category.nameUz=?2 or category.nameRu=?2)")
    Optional<InnerCategoryEntity> findByNameAndParentName(String innerCategoryName, String categoryName);


    @Query("from InnerCategoryEntity where nameUz=?1 or nameRu=?1")
    Optional<InnerCategoryEntity> findByName(String name);


}
