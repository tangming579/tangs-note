package com.tm.apm;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.List;

/**
 * @author: tangming
 * @date: 2022-06-11
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LearnDemoTest.class})
public class LearnDemoTest {
    @Test
    public void testSize() {
        Integer expected = 100;
        List list = PowerMockito.mock(List.class);
        PowerMockito.when(list.size()).thenReturn(expected);
        Integer actual = list.size();
        Assert.assertEquals("返回值不相等", expected, actual);
    }

    @Test
    public void testGet() {
        List<Integer> mockList = PowerMockito.mock(List.class);
        PowerMockito.doNothing().when(mockList).clear();
        mockList.clear();
        Mockito.verify(mockList).clear();

    }

    @Test
    public void hashMap(){
        // 创建一个 HashMap
        HashMap<String, Integer> prices = new HashMap<>();

        // 往HashMap中添加映射关系
        prices.put("Shoes", 180);
        prices.put("Bag", 300);
        prices.put("Pant", 150);
        System.out.println("HashMap: " + prices);

        // Shoes中的映射关系已经存在
        // Shoes并没有计算新值
        int shoePrice = prices.computeIfAbsent("Shoes", (key) -> 280);
        System.out.println("Price of Shoes: " + shoePrice);

        // 输出更新后的 HashMap
        System.out.println("Updated HashMap: " + prices);
    }
}
