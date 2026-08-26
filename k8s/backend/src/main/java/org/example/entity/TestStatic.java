package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@TableName("test_static")
public class TestStatic {
    @TableId
    @TableField("hash")
    private String hash;

    @TableField("URL")
    @JsonProperty("URL")
    private String url;

    @TableField("bundleId")
    private String bundleId;

    private String ascribe;

    @TableField("event_number")
    @JsonProperty("event_number")
    private Integer eventNumber;

    @TableField("exception_type")
    @JsonProperty("exception_type")
    private String exceptionType;

    @TableField("record_data")
    @JsonProperty("record_data")
    private String recordData;

    private String recorder;
    private String remark;

    @TableField("isOutput")
    private Integer isOutput;
}
