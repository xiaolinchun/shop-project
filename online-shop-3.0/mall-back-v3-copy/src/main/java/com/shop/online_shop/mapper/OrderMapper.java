package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 状态条件更新：只有当前状态在 fromStatuses 中才推进，保证严格按序、不可跳过、重复点击幂等。
     * 返回 0 表示状态已变化，调用方应拦截。
     */
    @Update("<script>"
            + "UPDATE orders SET status = #{to}, updated_at = #{now} WHERE id = #{id} AND status IN "
            + "<foreach collection='fromStatuses' item='status' open='(' separator=',' close=')'>#{status}</foreach>"
            + "</script>")
    int updateStatusFromAny(@Param("id") Integer id,
                            @Param("fromStatuses") List<String> fromStatuses,
                            @Param("to") String to,
                            @Param("now") LocalDateTime now);
}
