package com.jiber.backend.auth.service;

import com.jiber.backend.auth.config.*;
import com.jiber.backend.auth.dto.*;
import com.jiber.backend.auth.mapper.*;
import com.jiber.backend.auth.service.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.util.StringUtils;

public record RefreshRequestContext(
        String userAgent,
        String remoteAddress
) {
    public static RefreshRequestContext empty() {
        return new RefreshRequestContext(null, null);
    }

    public static RefreshRequestContext from(HttpServletRequest request) {
        return new RefreshRequestContext(request.getHeader("User-Agent"), request.getRemoteAddr());
    }

    byte[] remoteAddressBytes() {
        if (!StringUtils.hasText(remoteAddress)) {
            return null;
        }
        try {
            return InetAddress.getByName(remoteAddress).getAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
