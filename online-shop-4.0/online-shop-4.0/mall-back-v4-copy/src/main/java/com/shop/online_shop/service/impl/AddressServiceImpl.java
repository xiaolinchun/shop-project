package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.buyer.AddressDTO;
import com.shop.online_shop.entity.Address;
import com.shop.online_shop.mapper.AddressMapper;
import com.shop.online_shop.service.AddressService;
import com.shop.online_shop.vo.AddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    private static final int MAX_ADDRESS = 20;

    @Override
    public List<AddressVO> listMine(Integer buyerAccountId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getBuyerAccountId, buyerAccountId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt)
                .orderByDesc(Address::getId);
        List<AddressVO> list = new ArrayList<>();
        for (Address address : list(wrapper)) {
            list.add(toVO(address));
        }
        return list;
    }

    @Override
    @Transactional
    public Integer create(Integer buyerAccountId, AddressDTO dto) {
        validateRequired(dto);
        if (countMine(buyerAccountId) >= MAX_ADDRESS) {
            throw new BusinessException("地址数量已达上限（20）");
        }
        LocalDateTime now = LocalDateTime.now();
        Address address = new Address();
        address.setBuyerAccountId(buyerAccountId);
        address.setName(dto.getName().trim());
        address.setPhone(dto.getPhone().trim());
        address.setProvince(dto.getProvince().trim());
        address.setCity(dto.getCity().trim());
        address.setDistrict(dto.getDistrict().trim());
        address.setDetail(dto.getDetail().trim());
        address.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefault(buyerAccountId);
        }
        save(address);
        return address.getId();
    }

    @Override
    @Transactional
    public void update(Integer buyerAccountId, Integer id, AddressDTO dto) {
        Address address = getOwned(buyerAccountId, id);
        if (dto.getName() != null) {
            if (!StringUtils.hasText(dto.getName())) {
                throw new BusinessException("收货人姓名不能为空");
            }
            address.setName(dto.getName().trim());
        }
        if (dto.getPhone() != null) {
            if (!StringUtils.hasText(dto.getPhone())) {
                throw new BusinessException("手机号不能为空");
            }
            address.setPhone(dto.getPhone().trim());
        }
        address.setProvince(pick(dto.getProvince(), address.getProvince()));
        address.setCity(pick(dto.getCity(), address.getCity()));
        address.setDistrict(pick(dto.getDistrict(), address.getDistrict()));
        address.setDetail(pick(dto.getDetail(), address.getDetail()));
        validateComplete(address);
        if (dto.getIsDefault() != null) {
            address.setIsDefault(dto.getIsDefault());
            if (dto.getIsDefault()) {
                clearDefault(buyerAccountId);
            }
        }
        address.setUpdatedAt(LocalDateTime.now());
        updateById(address);
    }

    @Override
    @Transactional
    public void delete(Integer buyerAccountId, Integer id) {
        getOwned(buyerAccountId, id);
        removeById(id);
    }

    @Override
    @Transactional
    public void setDefault(Integer buyerAccountId, Integer id) {
        Address address = getOwned(buyerAccountId, id);
        clearDefault(buyerAccountId);
        address.setIsDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        updateById(address);
    }

    @Override
    public Address getOwned(Integer buyerAccountId, Integer id) {
        Address address = getById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!buyerAccountId.equals(address.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }
        return address;
    }

    @Override
    public Address getDefault(Integer buyerAccountId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getBuyerAccountId, buyerAccountId)
                .eq(Address::getIsDefault, true)
                .orderByDesc(Address::getId)
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    private long countMine(Integer buyerAccountId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getBuyerAccountId, buyerAccountId);
        return count(wrapper);
    }

    private void clearDefault(Integer buyerAccountId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getBuyerAccountId, buyerAccountId)
                .eq(Address::getIsDefault, true)
                .set(Address::getIsDefault, false);
        update(wrapper);
    }

    private void validateRequired(AddressDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("收货人姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new BusinessException("手机号不能为空");
        }
        if (!StringUtils.hasText(dto.getProvince())
                || !StringUtils.hasText(dto.getCity())
                || !StringUtils.hasText(dto.getDistrict())
                || !StringUtils.hasText(dto.getDetail())) {
            throw new BusinessException("地址信息不完整");
        }
    }

    private void validateComplete(Address address) {
        if (!StringUtils.hasText(address.getName())) {
            throw new BusinessException("收货人姓名不能为空");
        }
        if (!StringUtils.hasText(address.getPhone())) {
            throw new BusinessException("手机号不能为空");
        }
        if (!StringUtils.hasText(address.getProvince())
                || !StringUtils.hasText(address.getCity())
                || !StringUtils.hasText(address.getDistrict())
                || !StringUtils.hasText(address.getDetail())) {
            throw new BusinessException("地址信息不完整");
        }
    }

    private String pick(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }

    private AddressVO toVO(Address address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setName(address.getName());
        vo.setPhone(address.getPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetail(address.getDetail());
        vo.setIsDefault(address.getIsDefault());
        vo.setCreatedAt(address.getCreatedAt());
        return vo;
    }
}
