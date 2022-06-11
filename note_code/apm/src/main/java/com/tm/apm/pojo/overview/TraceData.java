package com.tm.apm.pojo.overview;

public class TraceData {
    private long allTrace;
    private long errorTrace;

    public TraceData() {
    }

    public long getAllTrace() {
        return this.allTrace;
    }

    public long getErrorTrace() {
        return this.errorTrace;
    }

    public void setAllTrace(long allTrace) {
        this.allTrace = allTrace;
    }

    public void setErrorTrace(long errorTrace) {
        this.errorTrace = errorTrace;
    }

}
