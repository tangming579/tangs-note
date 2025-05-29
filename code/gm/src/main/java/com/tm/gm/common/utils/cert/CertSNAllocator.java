package com.tm.gm.common.utils.cert;

import java.math.BigInteger;

public interface CertSNAllocator {
    BigInteger nextSerialNumber() throws Exception;
}
