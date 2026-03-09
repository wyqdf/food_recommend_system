package com.foodrecommend.letmecook.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ResponseDataBuilder {

    private ResponseDataBuilder() {
    }

    public static <T> Map<String, Object> page(PageResult<T> pageResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", pageResult == null ? null : pageResult.getList());
        data.put("total", pageResult == null ? 0L : pageResult.getTotal());
        data.put("page", pageResult == null ? 1 : pageResult.getPage());
        data.put("pageSize", pageResult == null ? 10 : pageResult.getPageSize());
        return data;
    }
}
