/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import static io.vavr.API.Try;
import static org.onap.dcae.common.publishing.VavrUtils.enhanceError;
import static org.onap.dcae.common.publishing.VavrUtils.f;
import static org.onap.dcae.common.publishing.VavrUtils.logError;
import static org.onap.dcae.configuration.Conversions.toList;

import io.vavr.CheckedRunnable;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigFilesFacade is used for reading and writing application properties and dmaap configuration.
 */
public class ConfigFilesFacade {

    private static final Logger log = LoggerFactory.getLogger(ConfigFilesFacade.class);

    private Path dmaapConfigPath;
    private Path propertiesPath;

    ConfigFilesFacade(Path propertiesPath, Path dMaaPConfigPath) {
        this.propertiesPath = propertiesPath;
        this.dmaapConfigPath = dMaaPConfigPath;
    }

    /**
     * Set new paths
     * @param propertiesFile application property file
     * @param dmaapConfigFile dmaap configuration file
     */
    public void setPaths(Path propertiesFile, Path dmaapConfigFile) {
        this.propertiesPath = propertiesFile;
        this.dmaapConfigPath = dmaapConfigFile;
    }

    Try<Map<String, String>> readCollectorProperties() {
        log.info(f("Reading collector properties from path: '%s'", propertiesPath));
        return Try(this::readProperties)
            .map(prop -> toList(prop.getKeys()).toMap(k -> k, k -> (String) prop.getProperty(k)))
            .mapFailure(enhanceError("Unable to read properties configuration from path '%s'", propertiesPath))
            .onFailure(logError(log))
            .peek(props -> log.info(f("Read following collector properties: '%s'", props)));
    }

    Try<JSONObject> readDMaaPConfiguration() {
        log.info(f("Reading DMaaP configuration from file: '%s'", dmaapConfigPath));
        return readFile(dmaapConfigPath)
            .recover(FileNotFoundException.class, __ -> "{}")
            .mapFailure(enhanceError("Unable to read DMaaP configuration from file '%s'", dmaapConfigPath))
            .flatMap(Conversions::toJson)
            .onFailure(logError(log))
            .peek(props -> log.info(f("Read following DMaaP properties: '%s'", props)));
    }

    Try<Void> writeDMaaPConfiguration(JSONObject dMaaPConfiguration) {
        log.info(f("Writing DMaaP configuration '%s' into file '%s'", dMaaPConfiguration, dmaapConfigPath));
        return writeFile(dmaapConfigPath, indentConfiguration(dMaaPConfiguration.toString()))
            .mapFailure(enhanceError("Could not save new DMaaP configuration to path '%s'", dmaapConfigPath))
            .onFailure(logError(log))
            .peek(__ -> log.info("Written successfully"));
    }


    Try<Void> writeProperties(Map<String, String> properties) {
        log.info(f("Writing properties configuration '%s' into file '%s'", properties, propertiesPath));
        return Try.run(saveProperties(properties))
            .mapFailure(enhanceError("Could not save properties to path '%s'", properties))
            .onFailure(logError(log))
            .peek(__ -> log.info("Written successfully"));
    }

    private Try<String> readFile(Path path) {
        return Try(() -> new String(Files.readAllBytes(path), StandardCharsets.UTF_8))
            .mapFailure(enhanceError("Could not read content from path: '%s'", path));
    }

    private Try<Void> writeFile(Path path, String content) {
        return Try.run(() -> Files.write(path, content.getBytes()))
            .mapFailure(enhanceError("Could not write content to path: '%s'", path));
    }

    private PropertiesConfiguration readProperties() throws ConfigurationException {
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        propertiesConfiguration.setDelimiterParsingDisabled(true);
        propertiesConfiguration.load(propertiesPath.toFile());
        return propertiesConfiguration;
    }

    private CheckedRunnable saveProperties(Map<String, String> properties) {
        return () -> {
            PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(propertiesPath.toFile());
            propertiesConfiguration.setEncoding(null);
            for (Tuple2<String, String> property : properties) {
                updateProperty(propertiesConfiguration, property);
            }
            propertiesConfiguration.save();
        };
    }

    private void updateProperty(PropertiesConfiguration propertiesConfiguration, Tuple2<String, String> property) {
        if (propertiesConfiguration.containsKey(property._1)) {
            propertiesConfiguration.setProperty(property._1, property._2);
        } else {
            propertiesConfiguration.addProperty(property._1, property._2);
        }
    }

    private String indentConfiguration(String configuration) {
        return new JSONObject(configuration).toString(4);
    }
}
