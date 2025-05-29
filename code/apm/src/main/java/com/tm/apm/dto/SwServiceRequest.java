package com.tm.apm.dto;

import lombok.Data;

import java.util.Set;

/**
 * @author tangming
 * @date 2022/6/16
 */
@Data
public class SwServiceRequest {

    /**
     * skywalking所属租户id
     */
    private String swTenantId;

    /**
     * skywalking所属集群
     */
    private String swClusterId;

    /**
     * skywalking所属项目
     */
    private String swNamespace;

    /**
     * 查询的租户id集合
     */
    private Set<String> tenantIds;

    /**
     * 查询的集群id集合
     */
    private Set<String> clusterIds;

    /**
     * 查询的项目id集合
     */
    private Set<String> projects;

    /**
     * 是否查询mTLS，默认不查
     */
    private boolean mTLS;
}
