package com.yucircle.controller;

import com.yucircle.dto.ApiResponse;
import com.yucircle.dto.CreateCommentRequest;
import com.yucircle.entity.Comment;
import com.yucircle.service.CommentService;
import com.yucircle.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<Comment> createComment(@RequestBody CreateCommentRequest request,
                                              HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(commentService.createComment(userId, request), "评论成功");
    }

    @GetMapping("/post/{postId}")
    public ApiResponse<List<Comment>> getComments(@PathVariable Long postId) {
        return ApiResponse.success(commentService.getCommentsByPostId(postId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteComment(@PathVariable Long id,
                                             HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        commentService.deleteComment(id, userId);
        return ApiResponse.success("删除成功");
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证token");
        }
        return jwtUtils.getUserIdFromToken(authHeader.substring(7));
    }
}
