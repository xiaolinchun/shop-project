package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.online_shop.enums.IntentStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("intent")
public class Intent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String buyerName;
    private String phone;
    private String token;
    private IntentStatusEnum status;
    
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer productId;
}