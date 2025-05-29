package com.tm.apm.controller;

import com.tm.apm.constant.ApmCommonConstant;
import com.tm.apm.dto.Step;
import com.tm.apm.dto.SwServiceRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author tangming
 * @date 2022/6/16
 */
@RestController
@RequestMapping(value = ApmCommonConstant.APM_SERVER_CONTEXT + "/trace")
@Api(value = "trace", tags = "调用链查询")
@Slf4j
public class TraceController {


}
