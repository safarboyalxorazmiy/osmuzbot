package com.example.repository;

import com.example.entity.PostPhotoEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostPhotoRepository extends CrudRepository<PostPhotoEntity, Long> {
    List<PostPhotoEntity> findByPostId(Long postId);
}
