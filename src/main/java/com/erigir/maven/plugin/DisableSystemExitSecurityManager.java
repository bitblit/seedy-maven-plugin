package com.erigir.maven.plugin;

import java.security.Permission;

/**
 * cweiss : 7/18/12 2:51 PM
 */
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
