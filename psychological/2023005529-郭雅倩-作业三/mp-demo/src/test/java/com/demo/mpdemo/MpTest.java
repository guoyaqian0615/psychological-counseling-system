package com.demo.mpdemo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.mpdemo.entity.User;
import com.demo.mpdemo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class MpTest {

    @Resource
    private UserMapper userMapper;

    // ===================== 1. 5个原生MP API（作业要求） =====================
    @Test
    void testInsert() {
        User user = new User();
        user.setName("测试");
        user.setAge(20);
        user.setEmail("test@qq.com");
        userMapper.insert(user);
        System.out.println("新增成功：" + user.getId());
    }

    @Test
    void testSelectById() {
        User user = userMapper.selectById(1L);
        System.out.println(user);
    }

    @Test
    void testUpdate() {
        User user = new User();
        user.setId(1L);
        user.setName("修改后");
        userMapper.updateById(user);
    }

    @Test
    void testDelete() {
        userMapper.deleteById(1L);
    }

    @Test
    void testPage() {
        Page<User> page = new Page<>(1,3);
        userMapper.selectPage(page,null);
        System.out.println("总条数："+page.getTotal());
        System.out.println("数据："+page.getRecords());
    }

    // ===================== 2. 自定义SQL =====================
    @Test
    void testCustomDelete() {
        userMapper.deleteUserById(2L);
    }

    @Test
    void testCustomInsert() {
        Map<String,Object> map = new HashMap<>();
        map.put("name","自定义插入");
        map.put("age",22);
        map.put("email","custom@qq.com");
        userMapper.insertUser(map);
    }

    // ===================== 3. 自定义分页+动态SQL =====================
    @Test
    void testCustomPage() {
        Page<User> page = new Page<>(1,2);
        userMapper.selectUserPage(page,"测",20);
        System.out.println(page.getRecords());
    }

    // ===================== 4. 一对一、一对多 =====================
    @Test
    void testOneToOne() {
        User user = userMapper.selectUserWithCard(1L);
        System.out.println("用户："+user.getName());
        System.out.println("身份证："+user.getIdCard());
    }

    @Test
    void testOneToMany() {
        User user = userMapper.selectUserWithOrders(1L);
        System.out.println("用户："+user.getName());
        System.out.println("订单："+user.getOrderList());
    }
}