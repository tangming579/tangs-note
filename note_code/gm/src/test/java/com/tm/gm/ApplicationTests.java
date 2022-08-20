package com.tm.gm;

import com.tm.gm.common.gm.JwtHelper;
import com.tm.gm.common.utils.SM2Util;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class ApplicationTests {
    @Test
    void signToken() {
        HashMap<String, String> map = new HashMap<>();
        map.put("test", "test");
        map.put("test4", "test");
        map.put("test5", "test");
        String token = JwtHelper.genToken(map);
        System.out.println(token);

        Map<String, String> map1 = JwtHelper.verifyToken(token);
        System.out.println(map1);
    }
}
