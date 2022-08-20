package com.tm.gm;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.tm.gm.common.gm.JwtHelper;
import com.tm.gm.common.gm.SMAlgorithm;
import com.tm.gm.common.utils.SM2Util;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.KeyPair;
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

    @Test
    void jwtTest() {
        try {
            KeyPair keyPair = SM2Util.generateKeyPair();
            BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
            BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();
            final SMAlgorithm ALGORITHM = SMAlgorithm.builder().publicKey(publicKey).privateKey(privateKey).build();
            String token = "";
            try {
                JWTCreator.Builder builder = JWT.create()
                        .withIssuer("tm");
                token = builder.sign(ALGORITHM);
                System.out.printf(token);
            } catch (IllegalArgumentException e) {
                System.out.printf("jwt生成失败", e);
            }

            JWTVerifier verifier = JWT.require(ALGORITHM).build();
            DecodedJWT jwt = verifier.verify(token);
            System.out.printf(jwt.getHeader());

        } catch (Exception e) {
            System.out.printf("error");
        }
    }

    @Test
    void jwtTest2() {
        try {
            AsymmetricCipherKeyPair keyPair = SM2Util.generateKeyPairParameter();
            ECPrivateKeyParameters priKey = (ECPrivateKeyParameters) keyPair.getPrivate();
            ECPublicKeyParameters pubKey = (ECPublicKeyParameters) keyPair.getPublic();

            byte[] SRC_DATA = "Hello tangming".getBytes();
            byte[] WITH_ID = "auth-server".getBytes();

            byte[] sign = SM2Util.sign(priKey, WITH_ID, SRC_DATA);
            System.out.println("SM2 sign with withId result:\n" + ByteUtils.toHexString(sign));
            byte[] rawSign = SM2Util.decodeDERSM2Sign(sign);
            sign = SM2Util.encodeSM2SignToDER(rawSign);
            System.out.println("SM2 sign with withId result:\n" + ByteUtils.toHexString(sign));
            boolean flag = SM2Util.verify(pubKey, WITH_ID, SRC_DATA, sign);
            if (!flag) {
                Assert.fail("sign with withId verify failed");
            }

            sign = SM2Util.sign(priKey, SRC_DATA);
            System.out.println("SM2 sign without withId result:\n" + ByteUtils.toHexString(sign));
            flag = SM2Util.verify(pubKey, SRC_DATA, sign);
            if (!flag) {
                Assert.fail("sign without withId verify failed");
            }

        } catch (Exception e) {
            System.out.printf("error");
        }
    }
}
