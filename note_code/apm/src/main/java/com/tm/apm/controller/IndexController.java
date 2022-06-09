package com.tm.apm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tangming
 * @date 2022/6/9
 */
@RestController
@RequestMapping("/index")
public class IndexController {

    @GetMapping()
    public String Test(){
        return "hello";
    }
}
