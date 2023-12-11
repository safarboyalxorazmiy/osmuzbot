package com.example.service;

import com.example.entity.CategoryEntity;
import com.example.entity.InnerCategoryEntity;
import com.example.exceptions.CategoryNameFoundException;
import com.example.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void create(String nameUz, String nameRu) {
        CategoryEntity entity = new CategoryEntity();
        Optional<CategoryEntity> byNameUz = categoryRepository.findByNameUz(nameUz);
        if (byNameUz.isPresent()) {
            throw new CategoryNameFoundException("Category Uz already exists");
        }

        Optional<CategoryEntity> byNameRu = categoryRepository.findByNameRu(nameRu);
        if (byNameRu.isPresent()) {
            throw new CategoryNameFoundException("Category Ru already exists");
        }

        entity.setNameUz(nameUz);
        entity.setNameRu(nameRu);
        categoryRepository.save(entity);
    }

    public List<String> getAllUz () {
        Iterable<CategoryEntity> all = categoryRepository.findAll();

        List<String> result = new LinkedList<>();
        for (CategoryEntity categoryEntity : all) {
            result.add(categoryEntity.getNameUz());
        }

        return result;
    }

    public List<String> getAllRu () {
        Iterable<CategoryEntity> all = categoryRepository.findAll();

        List<String> result = new LinkedList<>();
        for (CategoryEntity categoryEntity : all) {
            result.add(categoryEntity.getNameRu());
        }

        return result;
    }

    public Boolean findByName(String name) {
        Optional<CategoryEntity> byName = categoryRepository.findByName(name);
        if (byName.isEmpty()) {
            return false;
        }

        return true;
    }

    public void init() {
        create("1-kichik nohiya", "1-микрорайон");
        create("2-kichik nohiya", "2-микрорайон");
        create("3-kichik nohiya", "3-микрорайон");
        create("4-kichik nohiya", "4-микрорайон");
        create("5-kichik nohiya", "5-микрорайон");
        create("Hovlilar", "Участки");
    }
}
