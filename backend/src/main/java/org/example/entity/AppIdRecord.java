package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("appid")
public class AppIdRecord {
    @TableId
    @TableField("bundleId")
    private String bundleId;

    @TableField("appid")
    private Long appId;
}
