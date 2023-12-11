package com.example.service;

import com.example.dto.InnerCategoryDTO;
import com.example.entity.CategoryEntity;
import com.example.entity.InnerCategoryEntity;
import com.example.exceptions.CategoryNotFoundException;
import com.example.exceptions.InnerCategoryNameFoundException;
import com.example.repository.CategoryRepository;
import com.example.repository.InnerCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
public class InnerCategoryService {
    private final CategoryRepository categoryRepository;
    private final InnerCategoryRepository innerCategoryRepository;

    public InnerCategoryService(InnerCategoryRepository innerCategoryRepository, CategoryRepository categoryRepository) {
        this.innerCategoryRepository = innerCategoryRepository;
        this.categoryRepository = categoryRepository;
    }

    public void create(String categoryName, String nameUz, String nameRu) {
        InnerCategoryEntity innerCategory = new InnerCategoryEntity();
        innerCategory.setCategoryId(getIdByCategoryName(categoryName));

        innerCategory.setNameUz(nameUz);
        innerCategory.setNameRu(nameRu);

        innerCategoryRepository.save(innerCategory);
    }

    public List<InnerCategoryDTO> getAllUz(String categoryName) {
        List<InnerCategoryDTO> result = new LinkedList<>();

        List<InnerCategoryEntity> byCategoryId = innerCategoryRepository.findByCategoryId(getIdByCategoryName(categoryName));
        for (InnerCategoryEntity innerCategory : byCategoryId) {
            InnerCategoryDTO innerCategoryDTO = new InnerCategoryDTO();
            innerCategoryDTO.setId(innerCategory.getId());
            innerCategoryDTO.setName(innerCategory.getNameUz());
            result.add(innerCategoryDTO);
        }
        return result;
    }

    public List<InnerCategoryDTO> getAllRu(String categoryName) {
        List<InnerCategoryDTO> result = new LinkedList<>();

        List<InnerCategoryEntity> byCategoryId = innerCategoryRepository.findByCategoryId(getIdByCategoryName(categoryName));
        for (InnerCategoryEntity innerCategory : byCategoryId) {
            InnerCategoryDTO innerCategoryDTO = new InnerCategoryDTO();
            innerCategoryDTO.setId(innerCategory.getId());
            innerCategoryDTO.setName(innerCategory.getNameRu());
            result.add(innerCategoryDTO);
        }
        return result;
    }

    public String getInnerCategoryIdByNameAndCategoryName(String innerCategoryName, String categoryName) {
        Optional<InnerCategoryEntity> byName = innerCategoryRepository.findByNameAndParentName(innerCategoryName, categoryName);
        if (byName.isEmpty()) {
            throw new CategoryNotFoundException("Category not found by this name: " + innerCategoryName);
        }
        InnerCategoryEntity category = byName.get();

        return category.getId();
    }

    public Boolean findByNameAndCategoryName(String name, String innerCategoryName) {
        Optional<InnerCategoryEntity> byName = innerCategoryRepository.findByNameAndParentName(name, innerCategoryName);
        if (byName.isEmpty()) {
            return false;
        }
        InnerCategoryEntity category = byName.get();

        return true;
    }

    public Long getIdByCategoryName(String categoryName) {
        Optional<CategoryEntity> byName = categoryRepository.findByName(categoryName);
        if (byName.isEmpty()) {
            throw new CategoryNotFoundException("Category not found by this name: " + categoryName);
        }
        CategoryEntity category = byName.get();

        return category.getId();
    }

    public void init() {
        create("1-kichik nohiya", "1-2 xonali", "1-2 комнатные");
        create("1-kichik nohiya", "3-4 xonali", "3-4 комнатные");
        create("1-kichik nohiya", "5+ xonali", "5+ комнатные");

        create("2-kichik nohiya", "1-2 xonali", "1-2 комнатные");
        create("2-kichik nohiya", "3-4 xonali", "3-4 комнатные");
        create("2-kichik nohiya", "5+ xonali", "5+ комнатные");

        create("3-kichik nohiya", "1-2 xonali", "1-2 комнатные");
        create("3-kichik nohiya", "3-4 xonali", "3-4 комнатные");
        create("3-kichik nohiya", "5+ xonali", "5+ комнатные");

        create("4-kichik nohiya", "1-2 xonali", "1-2 комнатные");
        create("4-kichik nohiya", "3-4 xonali", "3-4 комнатные");
        create("4-kichik nohiya", "5+ xonali", "5+ комнатные");

        create("5-kichik nohiya", "1-2 xonali", "1-2 комнатные");
        create("5-kichik nohiya", "3-4 xonali", "3-4 комнатные");
        create("5-kichik nohiya", "5+ xonali", "5+ комнатные");

        create("Hovlilar", "1-sektor", "1-сектор");
        create("Hovlilar", "2-sektor", "2-сектор");
        create("Hovlilar", "3-sektor", "3-сектор");
        create("Hovlilar", "4-sektor", "4-сектор");

    }
}
