package com.tm.apm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultDto<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    String msg;
    T rule;

    public ResultDto(String code, String msg){
        this.msg = msg;
    }

}
