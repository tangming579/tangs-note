package com.tm.apm.controller;

import com.tm.apm.constant.ApmCommonConstant;
import com.tm.apm.dto.ResultDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author: tangming
 * @date: 2022-06-11
 */
@Api(value = "OverviewController", tags = "总览")
@Slf4j
@RestController
@RequestMapping(ApmCommonConstant.APM_SERVER_CONTEXT + "/mesh/overview/clusters/{clusterId}")
public class OverviewController {

    @ApiOperation("集群维度")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "clusterId", value = "集群id", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "tenants", value = "租户", required = true, dataType = "String", paramType = "body")})
    @PostMapping("/call")
    public ResultDto<Map<String, Object>> getClusterData(@PathVariable String clusterId, @RequestBody List<String> tenants) {

        ResultDto<Map<String, Object>> resultDto = new ResultDto<>();
        if (CollectionUtils.isEmpty(tenants)) {
            resultDto.setMsg("暂无数据");
            return resultDto;
        }
        return resultDto;
    }
}
