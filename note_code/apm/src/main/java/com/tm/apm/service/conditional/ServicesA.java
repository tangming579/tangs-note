package com.tm.apm.service.conditional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("'${storageType}'.equals('A')")
public class ServicesA extends AbstractService {
    @Override
    public String showMessage() {
        return "ServiceA";
    }
}

