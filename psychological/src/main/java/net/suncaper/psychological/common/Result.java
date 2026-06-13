package net.suncaper.psychological.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**code：状态码，约定业务 / 请求状态
 200：成功
 500：业务 / 服务异常
 msg：提示信息（成功文案、错误描述）
 data：泛型 T，存放接口实际要返回的业务数据（可以是对象、集合、基本类型）*/
//统一接口结果返回分装类
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    // 成功（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 成功（不带数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 失败
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
}