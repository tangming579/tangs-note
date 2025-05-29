package com.tm.apm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(TmfConfiguration.PREFIX)
public class TmfConfiguration {
    public static final String PREFIX = "tmf";

    private String sharedNamespace = "system-mf";

    private String paasApiAddr = "";

    private String paasApiSignature = "8e059c94-f760-4f85-8910-f94c27cf0ff5";

    private boolean debug = false;

    private String alarmPaasUrl;
}
