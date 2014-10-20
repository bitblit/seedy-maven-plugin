package com.erigir.maven.plugin;

import com.erigir.maven.plugin.validator.JSONValidator;
import com.erigir.maven.plugin.validator.Validator;
import com.erigir.maven.plugin.validator.XMLValidator;

/**
 * Created by maxkeene on 10/10/14.
 */
public class ValidatorSetting {
    private ValidatorType type;
    private String includeRegex;

    public ValidatorType getType() {
        return type;
    }

    public void setType(ValidatorType type) {
        this.type = type;
    }

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }

    public static enum ValidatorType {
        XML (new XMLValidator()),
        JSON (new JSONValidator());

        private final Validator validatorInstance;

        ValidatorType(Validator validatorInstance) {
            this.validatorInstance = validatorInstance;
        }

        public Validator getValidatorInstance() {
            return this.validatorInstance;
        }
    }

}
