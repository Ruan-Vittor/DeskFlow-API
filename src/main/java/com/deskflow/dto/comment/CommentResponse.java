package com.deskflow.dto.comment;

import com.deskflow.dto.user.UserSummaryResponse;
import java.time.Instant;

public record CommentResponse(
        Long id,
        Long ticketId,
        UserSummaryResponse author,
        String content,
        Instant createdAt
) {
}
