package com.phototransform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 发送验证码请求 DTO
 *
 * 封装用户请求发送短信验证码所需的手机号参数。
 *
 * @see com.phototransform.controller.AuthController
 * @see com.phototransform.service.AuthService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
