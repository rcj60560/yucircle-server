package com.yucircle.controller;

import com.yucircle.dto.ApiResponse;
import com.yucircle.dto.CreatePostRequest;
import com.yucircle.dto.PageResult;
import com.yucircle.dto.UpdatePostRequest;
import com.yucircle.entity.Post;
import com.yucircle.service.PostService;
import com.yucircle.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<Post> createPost(@RequestBody CreatePostRequest request,
                                        HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Post post = postService.createPost(userId, request);
        return ApiResponse.success(post, "发帖成功");
    }

    @GetMapping
    public ApiResponse<PageResult<Post>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(postService.listPosts(page, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<Post> getPost(@PathVariable Long id) {
        return ApiResponse.success(postService.getPost(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Post> updatePost(@PathVariable Long id,
                                        @RequestBody UpdatePostRequest request,
                                        HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return ApiResponse.success(postService.updatePost(id, userId, request), "更新成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePost(@PathVariable Long id,
                                          HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        postService.deletePost(id, userId);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/search")
    public ApiResponse<List<Post>> searchPosts(@RequestParam String keyword) {
        return ApiResponse.success(postService.searchPosts(keyword));
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证token");
        }
        return jwtUtils.getUserIdFromToken(authHeader.substring(7));
    }
}
