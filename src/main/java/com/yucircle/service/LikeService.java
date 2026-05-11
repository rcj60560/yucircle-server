package com.yucircle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yucircle.dto.LikeRequest;
import com.yucircle.dto.LikeStatsResponse;
import com.yucircle.entity.PostLike;
import com.yucircle.mapper.PostLikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final PostLikeMapper postLikeMapper;
    private final PostService postService;

    public String toggleLike(Long userId, LikeRequest request) {
        String objectType = request.getObjectType();
        Long objectId = request.getObjectId();
        String type = request.getType();

        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getObjectType, objectType)
                .eq(PostLike::getObjectId, objectId)
                .eq(PostLike::getUserId, userId);

        PostLike existing = postLikeMapper.selectOne(wrapper);

        if (existing != null) {
            if (existing.getType().equals(type)) {
                // 取消
                postLikeMapper.deleteById(existing.getId());
                if ("post".equals(objectType)) {
                    postService.updateLikeCount(objectId, type, -1);
                }
                return "cancelled";
            } else {
                // 切换
                String oldType = existing.getType();
                existing.setType(type);
                postLikeMapper.updateById(existing);
                if ("post".equals(objectType)) {
                    postService.updateLikeCount(objectId, oldType, -1);
                    postService.updateLikeCount(objectId, type, 1);
                }
                return "switched";
            }
        } else {
            PostLike like = new PostLike();
            like.setObjectType(objectType);
            like.setObjectId(objectId);
            like.setUserId(userId);
            like.setType(type);
            like.setCreatedAt(LocalDateTime.now());
            postLikeMapper.insert(like);
            if ("post".equals(objectType)) {
                postService.updateLikeCount(objectId, type, 1);
            }
            return "liked";
        }
    }

    public LikeStatsResponse getStats(String objectType, Long objectId, Long userId) {
        LambdaQueryWrapper<PostLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(PostLike::getObjectType, objectType)
                .eq(PostLike::getObjectId, objectId)
                .eq(PostLike::getType, "like");
        long likeCount = postLikeMapper.selectCount(likeWrapper);

        LambdaQueryWrapper<PostLike> dislikeWrapper = new LambdaQueryWrapper<>();
        dislikeWrapper.eq(PostLike::getObjectType, objectType)
                .eq(PostLike::getObjectId, objectId)
                .eq(PostLike::getType, "dislike");
        long dislikeCount = postLikeMapper.selectCount(dislikeWrapper);

        String myAction = null;
        if (userId != null) {
            LambdaQueryWrapper<PostLike> myWrapper = new LambdaQueryWrapper<>();
            myWrapper.eq(PostLike::getObjectType, objectType)
                    .eq(PostLike::getObjectId, objectId)
                    .eq(PostLike::getUserId, userId);
            PostLike mine = postLikeMapper.selectOne(myWrapper);
            if (mine != null) {
                myAction = mine.getType();
            }
        }

        return new LikeStatsResponse(objectType, objectId, likeCount, dislikeCount, myAction);
    }
}
