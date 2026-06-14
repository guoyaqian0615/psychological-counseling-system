package com.example;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MyBatisTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testOneToOneAndOneToMany(){
        // 查询 id = 1 的用户（你数据库里的小明）
        User user = userMapper.selectUserWithAll(1L);

        System.out.println("==================== 结果 ====================");
        System.out.println("用户姓名：" + user.getName());
        System.out.println("身份证号：" + user.getIdCard().getCardNo());
        System.out.println("订单列表：" + user.getOrders());
        System.out.println("==============================================");
    }
}