package com.tm.apm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: tangming
 * @date: 2022-06-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDO {
    private String name;
    private Long id;
}
