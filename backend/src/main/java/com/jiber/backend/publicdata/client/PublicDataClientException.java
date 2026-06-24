package com.jiber.backend.publicdata.client;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

public class PublicDataClientException extends RuntimeException {

    public PublicDataClientException(String message) {
        super(message);
    }

    public PublicDataClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
