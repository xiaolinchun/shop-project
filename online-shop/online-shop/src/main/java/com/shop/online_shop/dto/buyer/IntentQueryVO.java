package com.shop.online_shop.dto.buyer;

import com.shop.online_shop.enums.IntentStatusEnum;
import lombok.Data;

@Data
public class IntentQueryVO {
    private IntentStatusEnum status;
    private Integer queueIndex; //前面还有几个人
    private Integer intentId;
}