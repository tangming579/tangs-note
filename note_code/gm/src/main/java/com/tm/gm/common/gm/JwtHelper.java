package com.tm.gm.common.gm;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.tm.gm.common.utils.BCECUtil;
import com.tm.gm.common.utils.SM2Util;
import com.tm.gm.common.utils.cert.SM2CertUtil;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.InputStream;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author tangming
 * @date 2022/8/19
 */
@Slf4j
public class JwtHelper {

    static {
        Security.addProvider(new BouncyCastleProvider());
        try {


        } catch (Exception e) {
            log.error("JWT工具初始化异常", e);
        }

    }

    public static void setKey(BCECPublicKey _publicKey, BCECPrivateKey _privateKey) {
        publicKey = _publicKey;
        privateKey = _privateKey;
    }

    /**
     * 设置发行人
     */
    private static final String ISSUER = "zzz";

    /**
     * SM2需要的公钥和私钥
     */
    private static BCECPublicKey publicKey;
    private static BCECPrivateKey privateKey;

    /**
     * 初始化SM3WithSM2算法
     */
    private static final SMAlgorithm ALGORITHM = SMAlgorithm.builder().publicKey(publicKey).privateKey(privateKey).build();

    /**
     * 生成jwt
     *
     * @param claims 携带的payload
     * @return jwt token
     */
    public static String genToken(Map<String, String> claims) {
        try {
            JWTCreator.Builder builder = JWT.create()
                    .withIssuer(ISSUER);
            claims.forEach(builder::withClaim);
            return builder.sign(ALGORITHM);
        } catch (IllegalArgumentException e) {
            log.error("jwt生成失败", e);
        }
        return null;
    }

    /**
     * 验签方法
     *
     * @param token jwt token
     * @return jwt payload
     */
    public static Map<String, String> verifyToken(String token) {
        JWTVerifier verifier = JWT.require(ALGORITHM).withIssuer(ISSUER).build();
        DecodedJWT jwt = verifier.verify(token);
        Map<String, Claim> map = jwt.getClaims();
        Map<String, String> resultMap = new HashMap<>();
        map.forEach((k, v) -> resultMap.put(k, v.asString()));
        return resultMap;
    }
}
