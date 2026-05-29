package com.phototransform.controller;

import com.phototransform.common.JwtUtil;
import com.phototransform.domain.entity.User;
import com.phototransform.domain.entity.UserQuota;
import com.phototransform.repository.UserQuotaRepository;
import com.phototransform.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

/**
 * Controller 集成测试基类。
 *
 * <p>统一注入 MockMvc / JWT / 用户仓储，提供用户+额度+token 创建。
 * 子类通过继承复用 setup，无需重复 User/Quota 初始化。</p>
 *
 * <p>环境：test profile（H2 + local storage + mock seedream）。
 * 需真实 API 的子类通过 {@code @ActiveProfiles({"test", "seedream-real"})} 覆盖。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserQuotaRepository userQuotaRepository;

    protected String authToken;

    @BeforeAll
    void setUpBase() {
        authToken = createUserAndGetToken("13900000000");
    }

    /**
     * 创建测试用户及额度，返回 JWT token
     */
    protected String createUserAndGetToken(String phone) {
        User user = User.builder()
                .phone(phone)
                .status(com.phototransform.enums.UserStatus.ACTIVE)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        UserQuota quota = UserQuota.builder()
                .userId(user.getId())
                .remaining(10)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        userQuotaRepository.save(quota);

        return jwtUtil.generateToken(user.getId());
    }
}
