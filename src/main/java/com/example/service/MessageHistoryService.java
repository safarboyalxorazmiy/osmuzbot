package com.example.service;

import com.example.entity.MessageHistoryEntity;
import com.example.repository.MessageHistoryRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class MessageHistoryService {
    private final MessageHistoryRepository messageHistoryRepository;

    public MessageHistoryService(MessageHistoryRepository messageHistoryRepository) {
        this.messageHistoryRepository = messageHistoryRepository;
    }

    public void create(Integer messageId, Long chatId) {
        MessageHistoryEntity messageHistoryEntity = new MessageHistoryEntity();
        messageHistoryEntity.setMessageId(messageId);
        messageHistoryEntity.setChatId(chatId);
        messageHistoryRepository.save(messageHistoryEntity);
    }

    public List<Integer> getAllPostIdByUserId(Long userId) {
        List<MessageHistoryEntity> byChatId = messageHistoryRepository.findByChatId(userId);

        List<Integer> result = new LinkedList<>();
        for (MessageHistoryEntity messageHistoryEntity : byChatId) {
            result.add(messageHistoryEntity.getMessageId());
        }

        return result;
    }

    public void deleteAllPostsByUserId(Long userId) {
        messageHistoryRepository.deleteByChatId(userId);
    }
}
