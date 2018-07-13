/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.dcae;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

/**
 * A little copying is SOMETIMES better than a little dependency.
 */
public class CLIUtils {
    public static final String kJvmSetting_FileRoot = "RRWT_FILES";

    public static URL findStream(String resourceName, Class<?> clazz) {
        try {
            File file = new File(resourceName);
            if (file.isAbsolute()) {
                return file.toURI().toURL();
            }
            final String filesRoot = System.getProperty(kJvmSetting_FileRoot, null);
            if (filesRoot != null) {
                final String fullPath = filesRoot + "/" + resourceName;
                file = new File(fullPath);
                if (file.exists()) {
                    return file.toURI().toURL();
                }
            }

            // next try the class's resource finder
            URL res = clazz.getClassLoader().getResource(resourceName);
            if (res != null) {
                return res;
            }

            // now try the system class loaders' resource finder
            res = ClassLoader.getSystemResource(resourceName);
            if (res != null) {
                return res;
            }
            throw new RuntimeException(String.format("Could not find a configuration file for resource name: '%s'", res));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not find a configuration file for resource name: '%s'", e));
        }

    }

    public static io.vavr.collection.HashMap<String, String> processCmdLine (String[] args)
    {
        final HashMap<String,String> map = new HashMap<String,String> ();

        String lastKey = null;
        for ( String arg : args )
        {
            if ( arg.startsWith ( "-" ) )
            {
                if ( lastKey != null )
                {
                    map.put ( lastKey.substring(1), "" );
                }
                lastKey = arg;
            }
            else
            {
                if ( lastKey != null )
                {
                    map.put ( lastKey.substring(1), arg );
                }
                lastKey = null;
            }
        }
        if ( lastKey != null )
        {
            map.put ( lastKey.substring(1), "" );
        }
        return io.vavr.collection.HashMap.ofAll(map);
    }

}
