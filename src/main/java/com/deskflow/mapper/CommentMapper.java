package com.deskflow.mapper;

import com.deskflow.dto.comment.CommentResponse;
import com.deskflow.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    private final UserMapper userMapper;

    public CommentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                userMapper.toSummary(comment.getAuthor()),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
