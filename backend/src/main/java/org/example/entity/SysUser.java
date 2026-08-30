package org.example.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data

@TableName("user")
public class SysUser {
    private String uid;
    private String name;
    private String pwd;
    private String type;
}
