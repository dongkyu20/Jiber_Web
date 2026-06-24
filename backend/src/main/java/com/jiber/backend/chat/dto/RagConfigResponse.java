package com.jiber.backend.chat.dto;

import com.jiber.backend.chat.client.*;
import com.jiber.backend.chat.dto.*;
import com.jiber.backend.chat.service.*;

public record RagConfigResponse(
        String embedding,
        int chunkSize,
        int overlap,
        boolean hybrid,
        boolean rerank
) {
}
