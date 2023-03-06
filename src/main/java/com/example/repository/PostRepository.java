package com.example.repository;

import com.example.entity.PostEntity;
import com.example.entity.PostPhotoEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends CrudRepository<PostEntity, Long> {
    @Query(value = "select * from post order by id desc limit 1;", nativeQuery = true)
    PostEntity getLast();

    List<PostEntity> findByCategoryId(String categoryId);
}
