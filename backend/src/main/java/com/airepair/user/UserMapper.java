package com.airepair.user;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper public interface UserMapper {
 @Select("SELECT id,username,password_hash,role,enabled FROM sys_user WHERE username=#{name}") User byName(String name);
 @Select("SELECT id,username,password_hash,role,enabled FROM sys_user WHERE id=#{id}") User byId(long id);
 @Insert("INSERT INTO sys_user(username,password_hash,role,enabled) VALUES(#{username},#{passwordHash},#{role},true)") @Options(useGeneratedKeys=true,keyProperty="id") int insert(User user);
 @Select("SELECT id,username,role,enabled FROM sys_user ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}") List<User> list(@Param("limit") int limit,@Param("offset") int offset);
 @Select("SELECT COUNT(*) FROM sys_user") long count();
 @Select("SELECT id,username,role,enabled FROM sys_user WHERE role='TECHNICIAN' AND enabled=true ORDER BY id") List<User> technicians();
 @Update("UPDATE sys_user SET role=#{role},updated_at=CURRENT_TIMESTAMP WHERE id=#{id}") int changeRole(@Param("id") long id,@Param("role") String role);
 @Select("SELECT user_id,building,room FROM dormitory WHERE user_id=#{id}") Dormitory getDormitory(long id);
 @Insert("INSERT INTO dormitory(user_id,building,room) VALUES(#{id},#{building},#{room}) ON DUPLICATE KEY UPDATE building=VALUES(building),room=VALUES(room),updated_at=CURRENT_TIMESTAMP") int saveDormitory(@Param("id") long id,@Param("building") String building,@Param("room") String room);
}
