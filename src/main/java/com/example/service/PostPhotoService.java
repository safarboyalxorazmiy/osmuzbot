package com.example.service;

import com.example.entity.PostPhotoEntity;
import com.example.repository.PostPhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostPhotoService {
    private final PostPhotoRepository postPhotoRepository;


    public PostPhotoService(PostPhotoRepository postPhotoRepository) {
        this.postPhotoRepository = postPhotoRepository;
    }

    public Boolean create(Long postId, String photoUrl) {
        PostPhotoEntity postPhotoEntity = new PostPhotoEntity();
        postPhotoEntity.setPostId(postId);
        postPhotoEntity.setPhotoUrl(photoUrl);
        postPhotoRepository.save(postPhotoEntity);

        return true;
    }

    public List<String> getPhotoUrl(Long postId) {
        List<String> urls = new ArrayList<>();

        List<PostPhotoEntity> byPostId = postPhotoRepository.findByPostId(postId);
        for (PostPhotoEntity postPhotoEntity : byPostId) {
            urls.add(postPhotoEntity.getPhotoUrl());
        }
        return urls;
    }
}
