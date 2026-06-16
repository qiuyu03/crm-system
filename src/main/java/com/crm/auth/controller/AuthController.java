package com.crm.auth.controller;

import com.crm.auth.dto.LoginDTO;
import com.crm.auth.dto.LoginVO;
import com.crm.auth.entity.User;
import com.crm.auth.mapper.UserMapper;
import com.crm.auth.util.JwtUtil;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return R.ok(new LoginVO(token, user.getUsername(), user.getRealName(),
                                user.getRole(), jwtUtil.getExpiration()));
    }

    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody LoginDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setEnabled(1);
        userMapper.insert(user);
        return R.ok();
    }
}
