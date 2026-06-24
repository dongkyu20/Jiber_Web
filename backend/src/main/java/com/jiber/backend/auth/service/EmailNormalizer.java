package com.jiber.backend.auth.service;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class EmailNormalizer {

    public String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
