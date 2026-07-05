package com.aiinput.admin.module.script.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("script_example")
public class ScriptExample {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sceneId;
    private String category;
    private String title;
    private String situation;
    private String theirMessage;
    private String aiReply;
    private String tags;
    private Integer useCount = 0;
    private Integer status = 1;
    @TableLogic
    private Integer deleted = 0;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
