package com.example.service;

import com.example.entity.PostEntity;
import com.example.exceptions.PostNotFoundException;
import com.example.repository.PostRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PostService {
    private final PostRepository postRepository;

    private final InnerCategoryService innerCategoryService;

    private final PostPhotoService postPhotoService;

    public PostService(PostRepository postRepository, InnerCategoryService innerCategoryService, PostPhotoService postPhotoService) {
        this.postRepository = postRepository;
        this.innerCategoryService = innerCategoryService;
        this.postPhotoService = postPhotoService;
    }

    public Long create(String content, String innerCategoryId) {
        PostEntity postEntity = new PostEntity();
        postEntity.setContent(content);
        postEntity.setCategoryId(innerCategoryId);
        postRepository.save(postEntity);

        return postEntity.getId();
    }

    public Boolean update(Long id, String content) {

        Optional<PostEntity> byId = postRepository.findById(id);
        if (byId.isEmpty()) {
            throw new PostNotFoundException("Post not found by this id");
        }

        PostEntity postEntity = byId.get();
        postEntity.setContent(content);
        postRepository.save(postEntity);
        return true;
    }

    public Boolean delete(Long id) {
        Optional<PostEntity> byId = postRepository.findById(id);
        if (byId.isEmpty()) {
            throw new PostNotFoundException("Post not found by this id");
        }

        PostEntity postEntity = byId.get();
        postRepository.deleteById(id);
        return true;
    }

    public Long getLastId() {
        return postRepository.getLast().getId();
    }

    public PostEntity getLast() {
        return postRepository.getLast();
    }

    public List<PostEntity> getPostsByInnerCategoryId(String from) {
        return postRepository.findByCategoryId(innerCategoryService.getInnerCategoryIdByName(from));
    }

    public PostEntity getPostById(Long postId) {
        return postRepository.findById(postId).get();
    }

    public void init() {
        postPhotoService.create(create("Qurilish va ta'mirlash xizmatlari !!!", "f788e88d-f11e-40c8-87f1-0cf2348fd435"), "https://static.review.uz/crop/9/3/825__95_933163210.jpg?v=1608176475");
    }
}
