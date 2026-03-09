package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class LogResponse {
    private Integer id;
    private Integer adminId;
    private String adminName;
    private String operation;
    private String module;
    private String content;
    private String ip;
    private String createTime;
}
