package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.Review;
import com.shop.online_shop.vo.ProductRatingRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT COALESCE(AVG(rating), 0) FROM review WHERE product_id = #{productId}")
    Double averageRating(@Param("productId") Integer productId);

    @Select("SELECT COUNT(*) FROM review WHERE product_id = #{productId}")
    int countByProduct(@Param("productId") Integer productId);

    @Select("<script>"
            + "SELECT product_id AS productId, AVG(rating) AS averageRating, COUNT(*) AS reviewCount "
            + "FROM review WHERE product_id IN "
            + "<foreach collection='productIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach> "
            + "GROUP BY product_id"
            + "</script>")
    List<ProductRatingRow> statsByProductIds(@Param("productIds") List<Integer> productIds);
}
