package com.example.repository;

import com.example.entity.UserEntity;
import com.example.enums.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, Long> {
    Optional<UserEntity> getUserByChatId(Long chatId);

    List<UserEntity> findByRole(Role role);
}
