package org.example.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.SysUser;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT uid, name, pwd, type FROM user WHERE uid = #{uid}")
    SysUser findByUid(@Param("uid") String uid);

    @Select("SELECT uid, name, type FROM user")
    List<SysUser> findAll();

    @Insert("INSERT INTO user (uid, name, pwd, type) VALUES (#{uid}, #{name}, #{pwd}, #{type})")
    int insertUser(SysUser user);

    @Delete("DELETE FROM user WHERE uid = #{uid}")
    int deleteByUid(@Param("uid") String uid);

    @Select("SELECT COUNT(*) FROM user WHERE uid = #{uid}")
    int existsByUid(@Param("uid") String uid);

    @Update("UPDATE user SET pwd = #{pwd} WHERE uid = #{uid}")
    int updatePassword(@Param("uid") String uid, @Param("pwd") String pwd);
}
