package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.AddressDTO;
import com.shop.online_shop.entity.Address;
import com.shop.online_shop.vo.AddressVO;

import java.util.List;

public interface AddressService extends IService<Address> {

    /** 2.21 地址列表（默认地址排首位，其余按创建时间倒序） */
    List<AddressVO> listMine(Integer buyerAccountId);

    /** 2.22 新增地址，返回地址 ID */
    Integer create(Integer buyerAccountId, AddressDTO dto);

    /** 2.23 编辑地址 */
    void update(Integer buyerAccountId, Integer id, AddressDTO dto);

    /** 2.24 删除地址 */
    void delete(Integer buyerAccountId, Integer id);

    /** 2.25 设为默认地址 */
    void setDefault(Integer buyerAccountId, Integer id);

    /** 获取本人地址（不存在 / 非本人 → 业务异常） */
    Address getOwned(Integer buyerAccountId, Integer id);

    /** 获取默认地址（无默认返回 null） */
    Address getDefault(Integer buyerAccountId);
}
