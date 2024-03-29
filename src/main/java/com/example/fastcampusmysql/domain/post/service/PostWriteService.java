package com.example.fastcampusmysql.domain.post.service;

import com.example.fastcampusmysql.domain.post.dto.PostCommand;
import com.example.fastcampusmysql.domain.post.entity.Post;
import com.example.fastcampusmysql.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PostWriteService {
    private final PostRepository postRepository;

    public Long create(PostCommand command) {
        Post post = Post
                .builder()
                .memberId(command.memberId())
                .contents(command.contents())
                .build();
        return postRepository.save(post).getId();
    }

    // todo: pessimistic lock
//    @Transactional
//    public void likePost(Long postId) {
//        Post post = postRepository.findById(postId, true).orElseThrow();
//        post.incrementLikeCount();
//        postRepository.save(post);
//    }

    @Transactional
    public void likePostByOptimisticLock(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.incrementLikeCount();
        postRepository.save(post);
    }
}
