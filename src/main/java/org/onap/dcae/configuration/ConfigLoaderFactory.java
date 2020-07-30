/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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
package org.onap.dcae.configuration;

import java.nio.file.Path;
import org.onap.dcae.VesApplication;
import org.onap.dcae.configuration.cbs.CbsConfigResolver;
import org.onap.dcae.configuration.cbs.CbsConfigResolverFactory;

public class ConfigLoaderFactory {

    public ConfigLoader create(Path propertiesFile, Path dmaapConfigFile) {
        ConfigFilesFacade configFilesFacade = new ConfigFilesFacade(propertiesFile, dmaapConfigFile);
        CbsConfigResolver cbsConfigResolver = new CbsConfigResolverFactory().createCbsClient();
        return new ConfigLoader(
            configFilesFacade,
            cbsConfigResolver,
            VesApplication::restartApplication);
    }
}