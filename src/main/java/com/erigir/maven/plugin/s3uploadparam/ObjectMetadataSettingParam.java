package com.erigir.maven.plugin.s3uploadparam;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.erigir.wrench.drigo.AddMetadata;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Copyright 2014 Christopher Weiss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
public class ObjectMetadataSettingParam {

    private static final String CONTENT_TYPE = "content-type";
    private static final String CACHE_CONTROL = "cache-control";
    private static final String CONTENT_DISPOSITION = "content-disposition";
    private static final String CONTENT_ENCODING = "content-encoding";
    private static final String CONTENT_MD5 = "md5-base64";

    private static final List<String> OMD_EXCLUDE= Arrays.asList("APPLIED");

    private String includeRegex;
    private String contentType;
    private String cacheControl;
    private String contentDisposition;
    private String contentEncoding;
    private String md5;
    private Map<String, String> userMetaData = new TreeMap<>();

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ObjectMetadataSettingParam[includeRegex=").append(includeRegex)
                .append(", contentType=").append(contentType)
                .append(", cacheControl=").append(cacheControl)
                .append(", contentDisposition=").append(contentDisposition)
                .append(", contentEncoding=").append(contentEncoding)
                .append(", userMetaData=").append(userMetaData)
                .append(", md5=").append(md5)
                .append("]");
        return sb.toString();
    }

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        if (includeRegex == null) {
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

    public void setMd5(String md5) {
        this.md5 = md5;
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

        log.debug("Tested " + f.getName() + " against " + includeRegex + " returning " + rval);
        return rval;
    }

    public static void populateObjectMetaDataFromDrigoMeta(ObjectMetadata omd, Map<String, String> drigoMeta) {
        for (Map.Entry<String, String> e : drigoMeta.entrySet()) {
            if (CONTENT_TYPE.equals(e.getKey())) {
                omd.setContentType(e.getValue());
            } else if (CACHE_CONTROL.equals(e.getKey())) {
                omd.setCacheControl(e.getValue());
            } else if (CONTENT_DISPOSITION.equals(e.getKey())) {
                omd.setContentDisposition(e.getValue());
            } else if (CONTENT_ENCODING.equals(e.getKey())) {
                omd.setContentEncoding(e.getValue());
            } else if (CONTENT_MD5.equals(e.getKey())){
                omd.setContentMD5(e.getValue());
            }
            else {
                omd.addUserMetadata(e.getKey(), e.getValue());
            }
        }
    }

    public List<AddMetadata> toAddMetadata() {
        List<AddMetadata> rval = new LinkedList<>();

        if (contentType != null) {
            rval.add(createAMD(CONTENT_TYPE, contentType));
        }
        if (cacheControl != null) {
            rval.add(createAMD(CACHE_CONTROL, cacheControl));
        }
        if (contentDisposition != null) {
            rval.add(createAMD(CONTENT_DISPOSITION, contentDisposition));
        }
        if (contentEncoding != null) {
            rval.add(createAMD(CONTENT_ENCODING, contentEncoding));
        }
        if (md5 != null) {
            rval.add(createAMD(CONTENT_MD5, md5));
        }
        for (Map.Entry<String, String> e : userMetaData.entrySet()) {
            if (!OMD_EXCLUDE.contains(e.getKey()))
            {
                rval.add(createAMD(e.getKey(), e.getValue()));
            }
        }


        return rval;
    }

    private AddMetadata createAMD(String name, String value) {
        AddMetadata rval = new AddMetadata();
        rval.setIncludeRegex(includeRegex);
        rval.setName(name);
        rval.setValue(value);
        return rval;
    }
}
