/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jasper.compiler;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.jasper.runtime.ExceptionUtils;

/**
 * Class responsible for converting error codes to corresponding localized error messages.
 *
 * @author Jan Luehe
 */
public class Localizer {

    private static ResourceBundle bundle;

    static {
        try {
            bundle = ResourceBundle.getBundle("org.apache.jasper.resources.LocalStrings");
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
        }
    }

    /*
     * Returns the localized error message corresponding to the given error code.
     *
     * If the given error code is not defined in the resource bundle for localized error messages, it is used as the
     * error message.
     *
     * @param errCode Error code to localize
     *
     * @return Localized error message
     */
    public static String getMessage(String errCode) {
        String errMsg = errCode;
        try {
            if (bundle != null) {
                errMsg = bundle.getString(errCode);
            }
        } catch (MissingResourceException e) {
            // Ignore
        }
        return errMsg;
    }

    /*
     * Returns the localized error message corresponding to the given error code.
     *
     * If the given error code is not defined in the resource bundle for localized error messages, it is used as the
     * error message.
     *
     * @param errCode Error code to localize
     *
     * @param args Arguments for parametric replacement
     *
     * @return Localized error message
     */
    public static String getMessage(String errCode, Object... args) {
        String errMsg = getMessage(errCode);

        if (args != null && args.length > 0) {
            MessageFormat formatter = new MessageFormat(errMsg);
            errMsg = formatter.format(args);
        }

        return errMsg;
    }
}
