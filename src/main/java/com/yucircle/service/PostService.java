package com.yucircle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yucircle.dto.CreatePostRequest;
import com.yucircle.dto.PageResult;
import com.yucircle.dto.UpdatePostRequest;
import com.yucircle.entity.Post;
import com.yucircle.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;

    public Post createPost(Long userId, CreatePostRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            post.setImages(String.join(",", request.getImages()));
        }
        post.setCategory(request.getCategory());
        post.setLikeCount(0);
        post.setDislikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setStatus("published");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.insert(post);
        return post;
    }

    public PageResult<Post> listPosts(int page, int limit) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, "published").orderByDesc(Post::getCreatedAt);
        long total = postMapper.selectCount(wrapper);
        LambdaQueryWrapper<Post> pageWrapper = new LambdaQueryWrapper<>();
        pageWrapper.eq(Post::getStatus, "published")
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT " + limit + " OFFSET " + ((page - 1) * limit));
        List<Post> records = postMapper.selectList(pageWrapper);
        return new PageResult<>(total, page, limit, records);
    }

    public Post getPost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new RuntimeException("帖子不存在");
        // 增加浏览量
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
        return post;
    }

    public Post updatePost(Long id, Long userId, UpdatePostRequest request) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new RuntimeException("帖子不存在");
        if (!post.getUserId().equals(userId)) throw new RuntimeException("无权修改他人帖子");
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getImages() != null) {
            post.setImages(String.join(",", request.getImages()));
        }
        if (request.getCategory() != null) post.setCategory(request.getCategory());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        return post;
    }

    public void deletePost(Long id, Long userId) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new RuntimeException("帖子不存在");
        if (!post.getUserId().equals(userId)) throw new RuntimeException("无权删除他人帖子");
        post.setStatus("deleted");
        postMapper.updateById(post);
    }

    public List<Post> searchPosts(String keyword) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, "published")
                .and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword))
                .orderByDesc(Post::getCreatedAt);
        return postMapper.selectList(wrapper);
    }

    public void incrementCommentCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);
        }
    }

    public void updateLikeCount(Long postId, String type, int delta) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            if ("like".equals(type)) {
                post.setLikeCount(Math.max(0, post.getLikeCount() + delta));
            } else {
                post.setDislikeCount(Math.max(0, post.getDislikeCount() + delta));
            }
            postMapper.updateById(post);
        }
    }
}
