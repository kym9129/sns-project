package com.example.fastcampusmysql.domain.post.service;

import com.example.fastcampusmysql.domain.post.dto.DailyPostCount;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCountRequest;
import com.example.fastcampusmysql.domain.post.dto.PostDto;
import com.example.fastcampusmysql.domain.post.entity.Post;
import com.example.fastcampusmysql.domain.post.repository.PostLikeRepository;
import com.example.fastcampusmysql.domain.post.repository.PostRepository;
import com.example.fastcampusmysql.util.CursorRequest;
import com.example.fastcampusmysql.util.PageCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PostReadService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    public List<DailyPostCount> getDailyPostCounts(DailyPostCountRequest request) {
        return postRepository.groupByCreatedDate(request);
    }

//    public Page<Post> getPosts(long memberId, PageRequest pageRequest) {
    public Page<PostDto> getPosts(long memberId, Pageable pageRequest) {
        return postRepository.findAllByMemberId(memberId, pageRequest)
                .map(this::toDto); // Page 인터페이스에 map이 있음
    }

    private PostDto toDto(Post post){
        return new PostDto(
                post.getId(),
                post.getContents(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                postLikeRepository.countByPostId(post.getId())// post 조회 때마다 매번 count 쿼리 발생
                // mapper의 로직들은 파라미터로 받는 것이 좋을 것 같음
                // 쓰기 성능을 얻고 읽기 성능을 (많이) 잃은 트레이드오프 사례
                // 개선 포인트
        );
    }

    public PageCursor<PostDto> getPosts(long memberId, CursorRequest cursorRequest) {
        validateSize(cursorRequest.size());
        List<Post> posts = findAllBy(memberId, cursorRequest);
        // 반환한 데이터에서 가장 작은 key값을 추출. 없으면 NONE_KEY 리턴
        Long nextKey = getNextKey(posts);
        List<PostDto> postDtos = posts.stream().map(this::toDto).toList();
        return new PageCursor<>(cursorRequest.next(nextKey), postDtos);
    }

    public List<Post> getPosts(List<Long> ids) {
        return postRepository.findByIdIn(ids);
    }

    public Post getPost(Long postId){
        return postRepository.findById(postId).orElseThrow();
    }

    public PageCursor<Post> getPosts(List<Long> memberIds, CursorRequest cursorRequest) {
        validateSize(cursorRequest.size());
        List<Post> posts = findAllBy(memberIds, cursorRequest);
        Long nextKey = getNextKey(posts);
        return new PageCursor<>(cursorRequest.next(nextKey), posts);
    }

    private void validateSize(int size) {
        if(size == 0) {
            throw new IllegalArgumentException("size는 1 이상이어야 합니다.");
        }
    }

    private long getNextKey(List<Post> posts) {
        return posts.stream()
                .mapToLong(Post::getId)
                .min()
                .orElse(CursorRequest.NONE_KEY);
    }

    private List<Post> findAllBy(long memberId, CursorRequest cursorRequest) {
        Pageable pageable = PageRequest.of(0, cursorRequest.size());
        if(cursorRequest.hasKey()) {
            return postRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, cursorRequest.key(), pageable);
        }
        return postRepository.findByMemberIdOrderByIdDesc(memberId, pageable);
    }

    private List<Post> findAllBy(List<Long> memberIds, CursorRequest cursorRequest) {
        Pageable pageable = PageRequest.of(0, cursorRequest.size());
        if(cursorRequest.hasKey()) {
            return postRepository.findAllByMemberIdInAndIdLessThanOrderByIdDesc(memberIds, cursorRequest.key(), pageable);
        }
        return postRepository.findByMemberIdInOrderByIdDesc(memberIds, pageable);
    }
}
