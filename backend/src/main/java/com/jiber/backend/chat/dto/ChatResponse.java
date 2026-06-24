package com.jiber.backend.chat.dto;

import com.jiber.backend.chat.client.*;
import com.jiber.backend.chat.dto.*;
import com.jiber.backend.chat.service.*;

import java.util.List;

public record ChatResponse(
        boolean available,
        String answer,
        List<ChatContextResponse> contexts,
        String model,
        RagConfigResponse ragConfig
) {
}
