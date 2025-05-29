package com.tm.gm.common.gm;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.util.Base64Utils;

import java.security.*;
import java.security.spec.*;

/**
 * @author: tangming
 * @date: 2022-08-20
 */
public class JwtSM2Util {

    private static final SM2Engine.Mode DIGEST = SM2Engine.Mode.C1C3C2;
    private static final String ALGORITHM = "SM2";
    /**
     * 私钥转换为 {@link ECPrivateKeyParameters}
     * @param key key
     * @return
     * @throws InvalidKeyException
     */
    public static ECPrivateKeyParameters privateKeyToParams(String algorithm, byte[] key) throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        Preconditions.checkNotNull(key, "key must be not null !");
        PrivateKey privateKey = generatePrivateKey(algorithm, key);
        return (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter(privateKey);
    }

    /**
     * 生成私钥
     * @param algorithm 算法
     * @param key       key
     * @return
     */
    public static PrivateKey generatePrivateKey(String algorithm, byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Preconditions.checkNotNull(algorithm, "algorithm must be not null !");
        Preconditions.checkNotNull(key, "key must be not null !");
        KeySpec keySpec = new PKCS8EncodedKeySpec(key);
        algorithm = getAlgorithmAfterWith(algorithm);
        return getKeyFactory(algorithm).generatePrivate(keySpec);
    }

    /**
     * 公钥转换为 {@link ECPublicKeyParameters}
     * @param key key
     * @return
     * @throws InvalidKeyException
     */
    public static ECPublicKeyParameters publicKeyToParams(String algorithm, byte[] key) throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        Preconditions.checkNotNull(key, "key must be not null !");
        PublicKey publicKey = generatePublicKey(algorithm, key);
        return (ECPublicKeyParameters) ECUtil.generatePublicKeyParameter(publicKey);
    }

    /**
     * 生成公钥
     * @param algorithm 算法
     * @param key       key
     * @return
     */
    public static PublicKey generatePublicKey(String algorithm, byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Preconditions.checkNotNull(algorithm, "algorithm must be not null !");
        Preconditions.checkNotNull(key, "key must be not null !");
        KeySpec keySpec = new X509EncodedKeySpec(key);
        algorithm = getAlgorithmAfterWith(algorithm);
        return getKeyFactory(algorithm).generatePublic(keySpec);
    }

    /**
     * 获取用于密钥生成的算法<br>
     * 获取XXXwithXXX算法的后半部分算法，如果为ECDSA或SM2，返回算法为EC
     * @param algorithm XXXwithXXX算法
     * @return 算法
     */
    private static String getAlgorithmAfterWith(String algorithm) {
        Preconditions.checkNotNull(algorithm, "algorithm must be not null !");
        int indexOfWith = StringUtils.lastIndexOfIgnoreCase(algorithm, "with");
        if (indexOfWith > 0) {
            algorithm = StringUtils.substring(algorithm, indexOfWith + "with".length());
        }
        if ("ECDSA".equalsIgnoreCase(algorithm) || ALGORITHM.equalsIgnoreCase(algorithm)) {
            algorithm = "EC";
        }
        return algorithm;
    }

    /**
     * 获取{@link KeyFactory}
     * @param algorithm 非对称加密算法
     * @return {@link KeyFactory}
     */
    private static KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        final Provider provider = new BouncyCastleProvider();
        return KeyFactory.getInstance(algorithm, provider);
    }

    /**
     * 加密
     * @param data      数据
     * @param publicKey 公钥
     * @return 加密之后的数据
     */
    public static byte[] encrypt(byte[] data, byte[] publicKey) throws Exception {
        CipherParameters pubKeyParameters = new ParametersWithRandom(publicKeyToParams(ALGORITHM, publicKey));
        SM2Engine engine = new SM2Engine(DIGEST);
        engine.init(true, pubKeyParameters);
        return engine.processBlock(data, 0, data.length);
    }

    /**
     * 解密
     * @param data       数据
     * @param privateKey 私钥
     * @return 解密之后的数据
     */
    public static byte[] decrypt(byte[] data, byte[] privateKey) throws Exception {
        CipherParameters privateKeyParameters = privateKeyToParams(ALGORITHM, privateKey);
        SM2Engine engine = new SM2Engine(DIGEST);
        engine.init(false, privateKeyParameters);
        byte[] byteDate = engine.processBlock(data, 0, data.length);
        return byteDate;
    }

    /**
     * 签名
     * @param data 数据
     * @return 签名
     */
    public static byte[] sign(byte[] data, byte[] privateKey) throws Exception {
        SM2Signer signer = new SM2Signer();
        CipherParameters param = new ParametersWithRandom(privateKeyToParams(ALGORITHM, privateKey));
        signer.init(true, param);
        signer.update(data, 0, data.length);
        return signer.generateSignature();
    }

    /**
     * 用公钥检验数字签名的合法性
     * @param data      数据
     * @param sign      签名
     * @param publicKey 公钥
     * @return 是否验证通过
     */
    public static boolean verify(byte[] data, byte[] sign, byte[] publicKey) throws Exception {
        SM2Signer signer = new SM2Signer();
        CipherParameters param = publicKeyToParams(ALGORITHM, publicKey);
        signer.init(false, param);
        signer.update(data, 0, data.length);
        return signer.verifySignature(sign);
    }

    /**
     * 生成公钥和私钥
     * @return
     * @throws Exception
     */
    public static KeyPair generateSm2KeyPair(){
        //使用标准名称创建EC参数生成的参数规范
        final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");

        // 获取一个椭圆曲线类型的密钥对生成器
        final KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            // 使用SM2算法域参数集初始化密钥生成器（默认使用以最高优先级安装的提供者的 SecureRandom 的实现作为随机源）
            // kpg.initialize(sm2Spec);

            // 使用SM2的算法域参数集和指定的随机源初始化密钥生成器
            kpg.initialize(sm2Spec, new SecureRandom());

            // 通过密钥生成器生成密钥对
            return kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        KeyPair keyPair = generateSm2KeyPair();

        //明文
        String plaintext = "test";
        //加密
        byte[] ciphertext = Base64Utils.encode(encrypt(plaintext.getBytes("utf-8"), keyPair.getPublic().getEncoded()));
        //生成签名
        byte[] signature = Base64Utils.encode(sign(plaintext.getBytes("utf-8"),keyPair.getPrivate().getEncoded()));
        System.out.println("ciphertext: " + new String(ciphertext));
        System.out.println("signature: " + new String(signature));
        //解密
        plaintext = new String(decrypt(Base64Utils.decode(ciphertext),keyPair.getPrivate().getEncoded()),"utf-8");
        //验签
        boolean result = verify(plaintext.getBytes("utf-8"),Base64Utils.decode(signature),keyPair.getPublic().getEncoded());
        System.out.println("plaintext: " + plaintext);
        System.out.println("verify result: " + result);
    }
}
