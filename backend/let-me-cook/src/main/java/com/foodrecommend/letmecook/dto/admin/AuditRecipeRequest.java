package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class AuditRecipeRequest {
    private Integer status;
    private String reason;
}
