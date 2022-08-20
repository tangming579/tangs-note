package com.tm.gm.common.gm;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureGenerationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tm.gm.common.utils.SM2Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

/**
 * @author tangming
 * @date 2022/8/19
 */
@Slf4j
public class SMAlgorithm extends Algorithm {

    private final BCECPublicKey publicKey;
    private final BCECPrivateKey privateKey;

    private static final byte JWT_PART_SEPARATOR = (byte) 46;

    protected SMAlgorithm(BCECPublicKey publicKey, BCECPrivateKey privateKey) {
        super("SM3WithSM2", "SM3WithSM2");
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        if (publicKey == null || privateKey == null) {
            throw new IllegalArgumentException("The Key Provider cannot be null.");
        }
    }

    /**
     * signature = base64encode(SM2(SM3(base64encode(jwt.header) + ‘.’ + base64encode(jwt.payload)), ‘SECRET_KEY’))
     *
     * @param headerBytes
     * @param payloadBytes
     * @return
     * @throws SignatureGenerationException
     */
    @Override
    public byte[] sign(byte[] headerBytes, byte[] payloadBytes) throws SignatureGenerationException {
        //base64encode(jwt.header) + ‘.’ + base64encode(jwt.payload))
        byte[] hash = combineSignByte(headerBytes, payloadBytes);
        byte[] signatureByte;
        try {
            signatureByte = SM2Util.sign(privateKey, hash);
        } catch (CryptoException e) {
            throw new SignatureGenerationException(this, e);
        }

        return signatureByte;
    }

    @Override
    public void verify(DecodedJWT jwt) throws SignatureVerificationException {
        String signature = jwt.getSignature();
        byte[] signatureBytes = Base64.decodeBase64(signature);
        byte[] data = combineSignByte(jwt.getHeader().getBytes(), jwt.getPayload().getBytes());
        try {
            Boolean verifyResult = SM2Util.verify(publicKey, data, signatureBytes);
            System.out.printf(String.valueOf(verifyResult));
        } catch (Exception e) {
            throw new SignatureVerificationException(this);
        }
    }

    @Override
    @Deprecated
    public byte[] sign(byte[] contentBytes) throws SignatureGenerationException {
        // 不支持该方法
        throw new RuntimeException("该方法已过时");
    }

    /**
     * 拼接签名部分 header + . + payload
     *
     * @param headerBytes  header
     * @param payloadBytes payload
     * @return bytes
     */
    private byte[] combineSignByte(byte[] headerBytes, byte[] payloadBytes) {
        // header + payload
        byte[] hash = new byte[headerBytes.length + payloadBytes.length + 1];
        System.arraycopy(headerBytes, 0, hash, 0, headerBytes.length);
        hash[headerBytes.length] = JWT_PART_SEPARATOR;
        System.arraycopy(payloadBytes, 0, hash, headerBytes.length + 1, payloadBytes.length);
        return hash;
    }

    /**
     * builder
     */
    public static class SMAlogrithmBuilder {
        private BCECPublicKey publicKey;
        private BCECPrivateKey privateKey;

        SMAlogrithmBuilder() {
        }

        public SMAlgorithm.SMAlogrithmBuilder publicKey(final BCECPublicKey publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public SMAlgorithm.SMAlogrithmBuilder privateKey(final BCECPrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public SMAlgorithm build() {
            return new SMAlgorithm(this.publicKey, this.privateKey);
        }
    }

    public static SMAlgorithm.SMAlogrithmBuilder builder() {
        return new SMAlgorithm.SMAlogrithmBuilder();
    }
}
