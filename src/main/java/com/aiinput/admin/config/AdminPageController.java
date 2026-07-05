package com.aiinput.admin.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 管理后台页面路由（Thymeleaf 模板）
 */
@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/scripts")
    public String scriptList() {
        return "script-list";
    }

    @GetMapping("/scripts/new")
    public String scriptNew() {
        return "script-form";
    }

    /** 根路径重定向到话术列表 */
    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/admin/scripts";
    }
}
