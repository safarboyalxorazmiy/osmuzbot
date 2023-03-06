package com.example.repository;

import com.example.entity.AdminHistoryEntity;
import com.example.enums.Action;
import com.example.enums.Label;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminHistoryRepository extends CrudRepository<AdminHistoryEntity, Long> {

    Optional<AdminHistoryEntity> findDistinctTop1ByAdminIdOrderByIdDesc(Long adminId);
    Optional<AdminHistoryEntity> findDistinctTop1ByActionAndLabelOrderByIdDesc(Action action, Label label);

    @Query(value = "select * from admin_history where action LIKE '%_OPENING' and admin_id = ?1 order by id desc limit 1;", nativeQuery = true)
    AdminHistoryEntity findLastOpened(Long adminId);
}
