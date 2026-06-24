package com.jiber.backend.chat.dto;

import com.jiber.backend.chat.client.*;
import com.jiber.backend.chat.dto.*;
import com.jiber.backend.chat.service.*;

public record ChatContextResponse(
        String source,
        String text
) {
}
