package com.foodrecommend.letmecook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionDTO {
    private String value;
    private String type;
    private String typeLabel;
}
