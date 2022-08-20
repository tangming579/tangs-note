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

import java.io.File;
import java.io.FileInputStream;
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

/**
 * 生成jwt的工具类，基于auth0.java-jwt封装
 * 签名算法使用SM3WithSM2
 * payload统一使用Map<String, String>类型
 *
 * @author Created by zkk on 2020/9/22
 **/
@Slf4j
public class JwtHelper {

    static {
        Security.addProvider(new BouncyCastleProvider());
        X509Certificate cert;
        try {
            String path = "H:\\Projects\\tangs-note\\note_code\\gm\\target\\";
            // 从yml中读取配置
            //InputStream streamCer =JwtHelper.class.getClassLoader().getResourceAsStream(path + "test.sm2.cer");
            //InputStream streamPri = JwtHelper.class.getClassLoader().getResourceAsStream(path + "test.sm2.pri");

            File f = new File(path + "test.sm2.cer");
            File f1 = new File(path + "test.sm2.pri");
            InputStream streamCer = new FileInputStream(f);
            InputStream streamPri = new FileInputStream(f1);

            int streamPriLen = Objects.requireNonNull(streamPri).available();

            cert = SM2CertUtil.getX509Certificate(streamCer);

            byte[] priKeyData = new byte[streamPriLen];
            streamPri.read(priKeyData);
            // 从证书中获取公钥，从私钥文件中获取私钥
            publicKey = SM2CertUtil.getBCECPublicKey(cert);
            privateKey = BCECUtil.convertSEC1ToBCECPrivateKey(priKeyData);

        } catch (Exception e) {
            log.error("JWT工具初始化异常", e);
        }

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
