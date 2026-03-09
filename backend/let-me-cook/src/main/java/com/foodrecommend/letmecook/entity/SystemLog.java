package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.util.Date;

@Data
public class SystemLog {
    private Integer id;
    private Integer adminId;
    private String adminName;
    private String operation;
    private String module;
    private Integer targetId;
    private String content;
    private String ip;
    private String userAgent;
    private Date createTime;
}
