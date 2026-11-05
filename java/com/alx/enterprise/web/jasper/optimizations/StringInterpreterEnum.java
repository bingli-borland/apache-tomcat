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
package com.alx.enterprise.web.jasper.optimizations;

import com.alx.enterprise.web.jasper.compiler.Localizer;
import com.alx.enterprise.web.jasper.compiler.StringInterpreterFactory.DefaultStringInterpreter;
import com.alx.enterprise.logging.internal.Log;
import com.alx.enterprise.logging.internal.LogFactory;

/**
 * Provides an optimised conversion of string values to Enums. It bypasses the PropertyEditorManager lookup for a
 * registered PropertyEditor.
 */
public class StringInterpreterEnum extends DefaultStringInterpreter {

    /**
     * Constructs a new StringInterpreterEnum.
     */
    public StringInterpreterEnum() {
    }

    // Can't be static
    private final Log log = LogFactory.getLog(StringInterpreterEnum.class);

    @Override
    protected String coerceToOtherType(Class<?> c, String s, boolean isNamedAttribute) {
        if (c.isEnum() && !isNamedAttribute) {
            try {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) c, s);
                return c.getName() + "." + enumValue.name();
            } catch (IllegalArgumentException iae) {
                if (log.isDebugEnabled()) {
                    log.debug(Localizer.getMessage("jsp.error.typeConversion", s, "Enum[" + c.getName() + "]"), iae);
                }
                // Continue and resolve the value at runtime
            }
        }

        return null;
    }
}
