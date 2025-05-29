package com.tm.apm.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import springfox.documentation.spring.web.json.Json;

import java.io.IOException;
import java.util.Map;

/**
 * @author tangming
 * @date 2022/6/9
 */
@Slf4j
public class IndexService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 创建索引
     */
    public void createIndex() {
        try {
            // 定义索引名称
            CreateIndexRequest request = new CreateIndexRequest("user");
            // 添加aliases，对比上述结构来理解
            String aliaseStr = "{\"user.aliases\":{}}";
            Map aliases = JSONUtil.parseObj(aliaseStr).toBean(Map.class);
            // 添加mappings，对比上述结构来理解
            String mappingStr = "{\"properties\":{\"name\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\"}}},\"sex\":{\"type\":\"keyword\"},\"age\":{\"type\":\"integer\"}}}";
            Map mappings = JSONUtil.parseObj(mappingStr).toBean(Map.class);
            // 添加settings，对比上述结构来理解
            String settingStr = "{\"index\":{\"number_of_shards\":\"9\",\"number_of_replicas\":\"2\"}}";
            Map settings = JSONUtil.parseObj(settingStr).toBean(Map.class);

            // 添加数据
            request.aliases(aliases);
            //request.mapping(mappings);
            request.settings(settings);

            // 发送请求到ES
            CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            // 处理响应结果
            System.out.println("添加索引是否成功：" + response.isAcknowledged());
        } catch (IOException e) {
            log.error("", e);
        }
    }

}
