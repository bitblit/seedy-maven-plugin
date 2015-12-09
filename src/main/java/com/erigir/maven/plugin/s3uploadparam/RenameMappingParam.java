package com.erigir.maven.plugin.s3uploadparam;

import com.erigir.wrench.drigo.RenameMapping;

/**
 * Created by chrweiss on 3/8/15.
 */
public class RenameMappingParam {
    private String src;
    private String dst;

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public RenameMapping toDrigo() {
        RenameMapping rval = new RenameMapping();
        rval.setSrc(src);
        rval.setDst(dst);
        return rval;
    }
}
