package com.jiber.backend.admin;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper {

    List<AdminUserRow> findUsers(
            @Param("request") AdminUserListRequest request,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countUsers(@Param("request") AdminUserListRequest request);

    AdminUserRow findById(@Param("userId") Long userId);

    int updateRole(
            @Param("userId") Long userId,
            @Param("role") String role
    );

    int updateEnabled(
            @Param("userId") Long userId,
            @Param("enabled") Boolean enabled
    );
}
