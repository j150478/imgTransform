package com.phototransform.config;

import com.phototransform.common.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类，注册认证拦截器。
 *
 * <p>对 {@code /api/photo/**}、{@code /api/seedream/**}、{@code /api/user/logout}、{@code /api/payment/**}
 * 路径进行 Token 认证，排除注册和登录接口。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 构造 Web MVC 配置。
     *
     * @param authInterceptor 认证拦截器
     */
    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 注册拦截器，配置认证路径白名单。
     *
     * <p>认证路径：
     * <ul>
     *   <li>{@code /api/photo/**} — 照片转换相关接口</li>
 *   <li>{@code /api/seedream/**} — 文生图相关接口</li>
     *   <li>{@code /api/user/logout} — 用户登出</li>
     *   <li>{@code /api/payment/**} — 支付相关接口</li>
     * </ul>
     * 排除路径：
     * <ul>
     *   <li>{@code /api/user/register} — 用户注册</li>
     *   <li>{@code /api/user/login} — 用户登录</li>
     * </ul></p>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/photo/**", "/api/seedream/**", "/api/user/logout", "/api/payment/**")
                .excludePathPatterns("/api/user/send-code", "/api/user/login");
    }
}
