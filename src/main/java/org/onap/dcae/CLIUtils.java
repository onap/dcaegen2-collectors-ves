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

import java.util.HashMap;

/**
 *  CLIUtils extracted from nsaServerLibrary this implementation will be removed once we switch to different API library
 */
public class CLIUtils {

    public static io.vavr.collection.HashMap<String, String> processCmdLine (String[] args) {
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
