package com.tm.apm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("${database.isEmbedded:false}")
public class ServicesB extends AbstractService{
    @Override
    public void showMessage() {
        System.out.println("ServiceB");
    }
}
