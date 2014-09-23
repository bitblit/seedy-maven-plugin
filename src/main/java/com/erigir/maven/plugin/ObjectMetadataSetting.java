package com.erigir.maven.plugin;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Created by chrweiss on 9/18/14.
 */
public class ObjectMetadataSetting {
    private String includeRegex;
    private String contentType;
    private String cacheControl;
    private String contentDisposition;
    private String contentEncoding;
    private Map<String, String> userMetaData = new TreeMap<>();

    public void update(ObjectMetadata omd) {
        if (contentType != null) {
            omd.setContentType(contentType);
        }
        if (cacheControl != null) {
            omd.setCacheControl(cacheControl);
        }
        if (contentDisposition != null) {
            omd.setContentDisposition(contentDisposition);
        }
        if (contentEncoding != null) {
            omd.setContentEncoding(contentEncoding);
        }
        if (userMetaData != null) {
            for (Map.Entry<String, String> e : userMetaData.entrySet()) {
                omd.addUserMetadata(e.getKey(), e.getValue());
            }
        }
    }

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        if (includeRegex==null)
        {
            throw new IllegalArgumentException("Cannot set includeRegex to null");
        }
        this.includeRegex = includeRegex;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public Map<String, String> getUserMetaData() {
        return userMetaData;
    }

    public void setUserMetaData(Map<String, String> userMetaData) {
        this.userMetaData = userMetaData;
    }

    public boolean shouldInclude(File f, Log log) {
        Pattern p = Pattern.compile(includeRegex);
        boolean rval = (p.matcher(f.getAbsolutePath()).matches());

        log.debug("Tested "+f.getName()+" against "+includeRegex+" returning "+rval);
        return rval;
    }

}
