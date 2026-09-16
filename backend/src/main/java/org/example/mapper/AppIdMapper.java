package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.AppIdRecord;

import java.util.List;

@Mapper
public interface AppIdMapper extends BaseMapper<AppIdRecord> {

    @Select("SELECT * FROM appid WHERE appid = #{appId}")
    AppIdRecord selectByAppId(@Param("appId") Long appId);

    @Select("SELECT * FROM appid WHERE bundleId = #{bundleId}")
    AppIdRecord selectByBundleId(@Param("bundleId") String bundleId);

    @Insert("INSERT INTO appid (bundleId, appid) VALUES (#{bundleId}, #{appId})")
    int insertAppId(AppIdRecord record);

    @Select("SELECT * FROM appid")
    List<AppIdRecord> selectAll();
}
