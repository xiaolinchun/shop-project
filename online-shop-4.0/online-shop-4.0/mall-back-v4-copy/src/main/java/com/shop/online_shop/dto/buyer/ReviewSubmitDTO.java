package com.shop.online_shop.dto.buyer;

import lombok.Data;

import java.util.List;

/**
 * 提交评价请求
 */
@Data
public class ReviewSubmitDTO {

    /** 评分 1~5 星（必填） */
    private Integer rating;

    /** 文字评价 ≤200 字（选填） */
    private String content;

    /** 图片 URL 0~3 张 */
    private List<String> images;
}
