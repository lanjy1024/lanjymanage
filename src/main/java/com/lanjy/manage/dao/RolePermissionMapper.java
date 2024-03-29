package com.lanjy.manage.dao;

import com.lanjy.manage.pojo.RolePermissionKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RolePermissionMapper {
    int deleteByPrimaryKey(RolePermissionKey key);

    int insert(RolePermissionKey record);

    int insertSelective(RolePermissionKey record);

	List<RolePermissionKey> findByRole(int roleId);
}