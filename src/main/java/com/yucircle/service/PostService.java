package com.yucircle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yucircle.dto.CreatePostRequest;
import com.yucircle.dto.PageResult;
import com.yucircle.dto.UpdatePostRequest;
import com.yucircle.dto.PostWithUserDto;
import com.yucircle.entity.Post;
import com.yucircle.entity.User;
import com.yucircle.mapper.PostMapper;
import com.yucircle.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;

    public Post createPost(Long userId, CreatePostRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImages(request.getImages());  // 直接赋值，images 已经是 String
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

    public PageResult<PostWithUserDto> listPosts(int page, int limit) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, "published").orderByDesc(Post::getCreatedAt);
        long total = postMapper.selectCount(wrapper);
        
        LambdaQueryWrapper<Post> pageWrapper = new LambdaQueryWrapper<>();
        pageWrapper.eq(Post::getStatus, "published")
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT " + limit + " OFFSET " + ((page - 1) * limit));
        List<Post> records = postMapper.selectList(pageWrapper);
        
        // 获取所有用户 ID，然后批量查询用户信息
        List<Long> userIds = records.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        
        // 初始化空的 userMap
        final Map<Long, User> userMap;
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = new java.util.HashMap<>();
        }
        
        // 组合成 PostWithUserDto
        List<PostWithUserDto> dtos = records.stream().map(post -> {
            PostWithUserDto dto = new PostWithUserDto();
            dto.setId(post.getId());
            dto.setUserId(post.getUserId());
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setImages(post.getImages());
            dto.setCategory(post.getCategory());
            dto.setLikeCount(post.getLikeCount());
            dto.setDislikeCount(post.getDislikeCount());
            dto.setCommentCount(post.getCommentCount());
            dto.setViewCount(post.getViewCount());
            dto.setStatus(post.getStatus());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setUpdatedAt(post.getUpdatedAt());
            
            // 填充用户信息
            User user = userMap.get(post.getUserId());
            if (user != null) {
                dto.setNickname(user.getNickname());
                dto.setAvatar(user.getAvatar());
            } else {
                dto.setNickname("匿名用户");
                dto.setAvatar("");
            }
            return dto;
        }).collect(Collectors.toList());
        
        return new PageResult<>(total, page, limit, dtos);
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
        if (request.getImages() != null) post.setImages(request.getImages());  // 直接赋值
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
