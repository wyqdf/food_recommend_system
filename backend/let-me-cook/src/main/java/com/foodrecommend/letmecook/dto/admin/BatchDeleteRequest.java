package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class BatchDeleteRequest {
    private Integer[] ids;
}
