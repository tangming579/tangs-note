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
    void contextLoads() {
    }

    @Test
    void signToken() {
        HashMap<String, String> map = new HashMap<>();
        map.put("test", "test");
        map.put("test4", "test");
        map.put("test5", "test");


        //String token = JwtHelper.genToken(map);
        //System.out.println(token);

        AsymmetricCipherKeyPair keyPair = SM2Util.generateKeyPairParameter();
        String privateKey = keyPair.getPrivate().toString();

        //JwtHelper.setKey(keyPair.getPublic(),keyPair.getPrivate().);
    }

    @Test
    void verifyToken() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJTTTNXaXRoU00yIn0.eyJ0ZXN0NCI6InRlc3QiLCJ0ZXN0NSI6InRlc3QiLCJ0ZXN0IjoidGVzdCIsImlzcyI6Inp6eiJ9.MEQCICkcIuJ3cOYCd2wKHOwnt9ZnGcM_6xrNgRy3Bzq905s9AiAc0zzNG4_OhxCCZHMCB9Bg8vSBcLnX5jU1JUS56Hb6fg";
        Map<String, String> map1 = JwtHelper.verifyToken(token);
        System.out.println(map1);
    }
}
