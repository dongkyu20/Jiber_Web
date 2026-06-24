package com.jiber.backend.publicdata.client;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

public class PublicDataParseException extends RuntimeException {

    public PublicDataParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
