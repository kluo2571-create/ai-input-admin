package com.aiinput.admin.module.script.controller;

import com.aiinput.admin.common.R;
import com.aiinput.admin.module.script.entity.ScriptExample;
import com.aiinput.admin.module.script.mapper.ScriptExampleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/script")
@RequiredArgsConstructor
public class ScriptExampleController {

    private final ScriptExampleMapper mapper;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @GetMapping("/list")
    public R<List<ScriptExample>> list(
            @RequestParam(required = false) Long sceneId,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<ScriptExample> q = new LambdaQueryWrapper<ScriptExample>()
                .eq(ScriptExample::getStatus, 1)
                .orderByDesc(ScriptExample::getCreatedAt);
        if (sceneId != null) q.eq(ScriptExample::getSceneId, sceneId);
        if (category != null && !category.isBlank()) q.eq(ScriptExample::getCategory, category);
        return R.ok(mapper.selectList(q));
    }

    @PostMapping("/create")
    public R<Long> create(@RequestBody ScriptExample script) {
        if (script.getTitle() == null || script.getTitle().isBlank())
            return R.fail("标题不能为空");
        if (script.getTheirMessage() == null || script.getTheirMessage().isBlank())
            return R.fail("对话内容不能为空");
        script.setId(null);
        script.setStatus(1);
        script.setUseCount(0);
        mapper.insert(script);
        return R.ok(script.getId());
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ScriptExample script) {
        ScriptExample exist = mapper.selectById(id);
        if (exist == null) return R.fail("示例不存在");
        script.setId(id);
        mapper.updateById(script);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return R.ok();
    }

    @PostMapping("/enrich")
    public R<ScriptExample> enrich(@RequestParam String content) throws IOException {
        if (content == null || content.isBlank()) return R.fail("请描述你想要的话术场景");
        if (deepseekApiKey == null || deepseekApiKey.isBlank())
            return R.fail("未配置 AI Key");

        String systemPrompt = """
            你是沟通话术设计师。严格按照JSON格式输出，不要多说任何话：
            {"title":"示例标题(10字内)","category":"love/sales/business/daily","situation":"什么情况下该这么回(1-2句)","theirMessage":"对方说了什么(多行对话,TA:和我:交替)","aiReply":"参考回复(1-2句,自然接地气)","tags":"逗号分隔标签"}
            """;

        String body = objectMapper.writeValueAsString(Map.of(
            "model", "deepseek-chat",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", "请生成：" + content)
            ),
            "temperature", 0.8,
            "max_tokens", 600
        ));

        Request req = new Request.Builder()
                .url("https://api.deepseek.com/v1/chat/completions")
                .header("Authorization", "Bearer " + deepseekApiKey)
                .header("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            String json = resp.body() != null ? resp.body().string() : "";
            String content2 = objectMapper.readTree(json)
                    .path("choices").get(0).path("message").path("content").asText("");

            int start = content2.indexOf('{'), end = content2.lastIndexOf('}');
            if (start < 0 || end < 0) return R.fail("AI未返回有效JSON");

            var node = objectMapper.readTree(content2.substring(start, end + 1));
            ScriptExample s = new ScriptExample();
            s.setTitle(node.path("title").asText("话术示例"));
            s.setCategory(node.path("category").asText("love"));
            s.setSituation(node.path("situation").asText(content));
            s.setTheirMessage(node.path("theirMessage").asText(""));
            s.setAiReply(node.path("aiReply").asText(""));
            s.setTags(node.path("tags").asText(""));
            s.setStatus(1);
            s.setUseCount(0);
            return R.ok(s);
        }
    }
}
