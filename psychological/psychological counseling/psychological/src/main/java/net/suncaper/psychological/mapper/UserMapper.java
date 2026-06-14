package net.suncaper.psychological.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.suncaper.psychological.entity.User;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 按账号查询学生（仅查询role=student的用户，保证学生端只能登录学生账号）
     * @param username 学号
     * @return 学生用户信息
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND role = 'student'")
    User selectStudentByUsername(@Param("username") String username);
}