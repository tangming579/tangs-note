package com.tm.apm.controller;

import com.tm.apm.pojo.overview.TraceData;
import com.tm.apm.service.conditional.AbstractService;
import com.tm.apm.service.IndexService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author tangming
 * @date 2022/6/9
 */
@Api(value = "IndexController", tags = "测试用")
@Slf4j
@RestController
@RequestMapping("/index")
public class IndexController {

    @Autowired
    private IndexService indexService;

    @Autowired
    private AbstractService abstractService;

    @ApiOperation("Test接口")
    @GetMapping()
    public String Test() {
        return "hello";
    }

    @ApiOperation("conditional接口")
    @GetMapping("conditional")
    public String ConditionalTest(){
        return abstractService.showMessage();
    }

    @ApiOperation("创建索引")
    @GetMapping("CreateIndex")
    public String CreateIndex(){
        indexService.createIndex();
        return "成功";
    }

    @ApiOperation("Trace数据")
    @ApiImplicitParams({@ApiImplicitParam(name = "tenantId", value = "租户ID", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "clusterId", value = "集群ID", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "project", value = "项目ID", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "interval", value = "区间", required = false, dataType = "String", paramType = "query")})
    @GetMapping("/level/all/traces")
    public TraceData traceData(@RequestParam(value = "tenantId", required = false) String tenantId,
                               @RequestParam(value = "clusterId", required = false) String clusterId, @RequestParam(value = "project", required = false) String project,
                               @RequestParam(value = "interval", defaultValue = "1d") String interval) {

        return null;
    }

    @GetMapping(value = "/traces/{traceId}/pods")
    @ApiOperation(value = "根据traceId查询调用链详情")
    public String getTracePods(String tenantId) {
        indexService.createIndex();
        return "success";
    }
}
