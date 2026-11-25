package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 买家评价（按订单明细单件、每明细一次，提交后不可修改 / 删除）
 */
@Data
@TableName(value = "review", autoResultMap = true)
public class Review {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private Integer orderItemId;

    private Integer productId;

    private Integer buyerAccountId;

    /** 评分 1~5 */
    private Integer rating;

    /** 文字评价 ≤200 字 */
    private String content;

    @TableField(value = "images", typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private LocalDateTime createdAt;
}
