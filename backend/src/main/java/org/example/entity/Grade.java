package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("grade")
public class Grade {
    @TableId
    @TableField("URL")
    private String url;

    @TableField("grade")
    private String grade;

    @TableField("recorder")
    private String recorder;

    @TableField("remark")
    private String remark;
}
