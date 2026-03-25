package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class AuditRequest {
    private Integer status;
    private String reason;
}
