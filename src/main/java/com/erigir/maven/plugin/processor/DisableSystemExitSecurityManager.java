package com.erigir.maven.plugin.processor;

import java.security.Permission;

/**
 Copyright 2014 Christopher Weiss

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
public class DisableSystemExitSecurityManager extends SecurityManager {

    private SecurityManager wrapped;

    public DisableSystemExitSecurityManager(SecurityManager wrapped) {
        this.wrapped = wrapped;
    }

    public void checkPermission(Permission permission) {
        //LOG.trace("Check permission:{}", permission);

        if (RuntimePermission.class.isAssignableFrom(permission.getClass())) {
            String name = permission.getName();
            if ("exitVM".equals(name) || "exitVM.0".equals(name)) {
                throw new TriedToCallSystemExitException();
            } else if (wrapped != null) {
                wrapped.checkPermission(permission);
            }
        }

    }

    public SecurityManager getWrapped() {
        return wrapped;
    }
}
