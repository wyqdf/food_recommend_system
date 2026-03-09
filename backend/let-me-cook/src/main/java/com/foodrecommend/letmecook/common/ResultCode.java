package com.foodrecommend.letmecook.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权/登录过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    LOGIN_ERROR(1001, "用户名或密码错误"),
    USER_DISABLED(1002, "用户已被禁用"),
    OLD_PASSWORD_ERROR(1003, "旧密码错误"),
    USERNAME_EXISTS(1004, "用户名已存在"),
    EMAIL_EXISTS(1005, "邮箱已存在"),
    
    RECIPE_NOT_FOUND(2001, "菜谱不存在"),
    CATEGORY_NOT_FOUND(2002, "分类不存在"),
    
    COMMENT_NOT_FOUND(3001, "评论不存在"),
    
    NOT_FAVORITED(4001, "未收藏该菜谱");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
