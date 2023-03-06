package com.example.service;

import com.example.entity.AdminHistoryEntity;
import com.example.enums.Action;
import com.example.enums.Label;
import com.example.repository.AdminHistoryRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminHistoryService {
    private final AdminHistoryRepository adminHistoryRepository;
    private final CategoryService categoryService;
    private final InnerCategoryService innerCategoryService;

    public AdminHistoryService(AdminHistoryRepository adminHistoryRepository, CategoryService categoryService, InnerCategoryService innerCategoryService) {
        this.adminHistoryRepository = adminHistoryRepository;
        this.categoryService = categoryService;
        this.innerCategoryService = innerCategoryService;
    }

    public void create(Long adminId, Action action, Label label, String value) {
        AdminHistoryEntity entity = new AdminHistoryEntity();
        entity.setAdminId(adminId);
        entity.setAction(action);
        entity.setLabel(label);
        entity.setValue(value);

        adminHistoryRepository.save(entity);
    }

    public Action getLastAction(Long adminId) {
        Optional<AdminHistoryEntity> top1ByAdminId = adminHistoryRepository.findDistinctTop1ByAdminIdOrderByIdDesc(adminId);
        if (top1ByAdminId.isPresent()) {
            AdminHistoryEntity entity = top1ByAdminId.get();
            return entity.getAction();
        }

        return null;
    }

    public Label getLastLabel(Long adminId) {
        Optional<AdminHistoryEntity> top1ByAdminId = adminHistoryRepository.findDistinctTop1ByAdminIdOrderByIdDesc(adminId);
        if (top1ByAdminId.isPresent()) {
            AdminHistoryEntity entity = top1ByAdminId.get();
            return entity.getLabel();
        }

        return null;
    }

    public void saveCategory() {
        Optional<AdminHistoryEntity> nameUzAsked = adminHistoryRepository.findDistinctTop1ByActionAndLabelOrderByIdDesc(Action.CATEGORY_CREATING, Label.CATEGORY_NAME_UZ_ASKED);
        Optional<AdminHistoryEntity> nameRuAsked = adminHistoryRepository.findDistinctTop1ByActionAndLabelOrderByIdDesc(Action.CATEGORY_CREATING, Label.CATEGORY_NAME_RU_ASKED);
        String nameUz = nameUzAsked.get().getValue();
        String nameRu = nameRuAsked.get().getValue();
        categoryService.create(nameUz, nameRu);
    }

    public void saveInnerCategory(String categoryName) {
        Optional<AdminHistoryEntity> nameUzAsked = adminHistoryRepository.findDistinctTop1ByActionAndLabelOrderByIdDesc(Action.INNER_CATEGORY_CREATING, Label.INNER_CATEGORY_NAME_UZ_ASKED);
        Optional<AdminHistoryEntity> nameRuAsked = adminHistoryRepository.findDistinctTop1ByActionAndLabelOrderByIdDesc(Action.INNER_CATEGORY_CREATING, Label.INNER_CATEGORY_NAME_RU_ASKED);
        String nameUz = nameUzAsked.get().getValue();
        String nameRu = nameRuAsked.get().getValue();
        innerCategoryService.create(categoryName, nameUz, nameRu);
    }

    public Action getLastOpened(Long chatId) {
        AdminHistoryEntity lastOpened = adminHistoryRepository.findLastOpened(chatId);
        return lastOpened.getAction();
    }

    public String getLastOpenedValue(Long chatId) {
        AdminHistoryEntity lastOpened = adminHistoryRepository.findLastOpened(chatId);
        return lastOpened.getValue();
    }
}
