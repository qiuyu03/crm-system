package com.crm.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private String username;
    private String realName;
    private String role;
    private long expiresIn;   // 过期时长，毫秒
}
