package com.example.repository;

import com.example.entity.UserHistoryEntity;
import com.example.enums.Label;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserHistoryRepository extends CrudRepository<UserHistoryEntity, Long> {
    @Query(value = "select * from user_history where user_id = ?1 order by id desc limit 1;", nativeQuery = true)
    Optional<UserHistoryEntity> getLast(Long chatId);

    @Query(value = "select * from user_history where label = ?1 and user_id = ?2 order by id desc limit 1;", nativeQuery = true)
    Optional<UserHistoryEntity> getLastByLabel(String label, Long chatId);
}
