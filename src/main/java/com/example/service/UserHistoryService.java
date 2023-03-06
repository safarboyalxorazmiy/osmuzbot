package com.example.service;

import com.example.entity.UserHistoryEntity;
import com.example.enums.Label;
import com.example.repository.UserHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserHistoryService {
    @Autowired
    private UserHistoryRepository userHistoryRepository;

    // TODO Foydalanuvchining oxirgi ochgan labelini olish
    public Label getLastLabelByChatId(Long chatId) {
        Optional<UserHistoryEntity> last = userHistoryRepository.getLast(chatId);
        if (last.isPresent()) {
            return last.get().getLabel();
        }

        return null;
    }

    // TODO Foydalanuvchining oxirgi ochgan category nomini olish
    public String getLastCategoryName(Long chatId) {
        Optional<UserHistoryEntity> last = userHistoryRepository.getLastByLabel(Label.CATEGORY_OPENED.name(), chatId);
        if (last.isEmpty()) {
            return null;
        }

        return last.get().getValue();
    }

    // TODO Foydalanuvchining oxirgi ochgan innerCategory nomini olish
    public String getLastInnerCategoryName(Long chatId) {
        Optional<UserHistoryEntity> last = userHistoryRepository.getLastByLabel(Label.INNERCATEGORY_OPENED.name(), chatId);
        if (last.isPresent()) {
            return last.get().getValue();
        }

        return null;
    }

    public String getLastOfferId(Long chatId) {
        Optional<UserHistoryEntity> last = userHistoryRepository.getLastByLabel(Label.OFFER_STARTED.name(), chatId);
        if (last.isPresent()) {
            return last.get().getValue();
        }

        return null;
    }

    public void create(Label label, long chatId, String value) {
        UserHistoryEntity entity = new UserHistoryEntity();
        entity.setLabel(label);
        entity.setUserId(chatId);
        entity.setValue(value);
        userHistoryRepository.save(entity);
    }
}
