package com.example.repository;

import com.example.entity.MessageHistoryEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageHistoryRepository extends CrudRepository<MessageHistoryEntity, Long> {
    List<MessageHistoryEntity> findByChatId(Long chatId);

    @Transactional
    @Modifying
    @Query("delete from MessageHistoryEntity where chatId = ?1")
    void deleteByChatId(Long chatId);
}
