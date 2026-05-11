package com.yucircle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yucircle.dto.CreateCommentRequest;
import com.yucircle.entity.Comment;
import com.yucircle.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostService postService;

    public Comment createComment(Long userId, CreateCommentRequest request) {
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setRootId(request.getRootId());
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        comment.setStatus("published");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        // 更新帖子评论数
        postService.incrementCommentCount(request.getPostId());
        return comment;
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, "published")
                .orderByAsc(Comment::getCreatedAt);
        return commentMapper.selectList(wrapper);
    }

    public void deleteComment(Long id, Long userId) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) throw new RuntimeException("评论不存在");
        if (!comment.getUserId().equals(userId)) throw new RuntimeException("无权删除他人评论");
        comment.setStatus("deleted");
        commentMapper.updateById(comment);
    }
}
