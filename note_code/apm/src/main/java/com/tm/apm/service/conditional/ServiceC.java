package com.tm.apm.service.conditional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * @author tangming
 * @date 2022/7/8
 */
@Slf4j
@Component
@ConditionalOnExpression("'${storageType}'.equals('C')")
public class ServiceC extends AbstractService {
    @Override
    public String showMessage() {
        return "ServiceC";
    }
}
