package com.shop.online_shop.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流状态查询结果：运单号 + 服务商 + 跟踪信息条目（时间倒序）
 */
@Data
public class ShippingVO {

    private String orderNo;
    private String logisticsProvider;
    private String waybillNo;
    private List<TrackingItem> tracking;

    @Data
    public static class TrackingItem {
        private String status;
        private String description;
        private LocalDateTime time;

        public TrackingItem(String status, String description, LocalDateTime time) {
            this.status = status;
            this.description = description;
            this.time = time;
        }

        public TrackingItem() {
        }
    }
}
