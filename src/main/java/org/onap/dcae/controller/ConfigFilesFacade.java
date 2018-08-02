/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae.controller;

import static io.vavr.API.Try;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.enhanceError;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.f;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.logError;
import static org.onap.dcae.controller.Conversions.toList;

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

class ConfigFilesFacade {

    private static Logger log = LoggerFactory.getLogger(ConfigFilesFacade.class);

    private final Path dMaaPConfigPath;
    private final Path propertiesPath;

    public ConfigFilesFacade(Path dMaaPConfigPath, Path propertiesPath) {
        this.dMaaPConfigPath = dMaaPConfigPath;
        this.propertiesPath = propertiesPath;
    }

    Try<Map<String, String>> readCollectorProperties() {
        log.info(f("Reading collector properties from path: '%s'", propertiesPath));
        return Try(() -> readProperties())
            .map(prop -> toList(prop.getKeys()).toMap(k -> k, k -> (String) prop.getProperty(k)))
            .mapFailure(enhanceError("Unable to read properties configuration from path '%s'", propertiesPath))
            .onFailure(logError(log))
            .peek(props -> log.info(f("Read following collector properties: '%s'", props)));
    }

    Try<JSONObject> readDMaaPConfiguration() {
        log.info(f("Reading DMaaP configuration from file: '%s'", dMaaPConfigPath));
        return readFile(dMaaPConfigPath)
            .recover(FileNotFoundException.class, __ -> "{}")
            .mapFailure(enhanceError("Unable to read DMaaP configuration from file '%s'", dMaaPConfigPath))
            .flatMap(Conversions::toJson)
            .onFailure(logError(log))
            .peek(props -> log.info(f("Read following DMaaP properties: '%s'", props)));
    }

    Try<Void> writeDMaaPConfiguration(JSONObject dMaaPConfiguration) {
        log.info(f("Writing DMaaP configuration '%s' into file '%s'", dMaaPConfiguration, dMaaPConfigPath));
        return writeFile(dMaaPConfigPath, indentConfiguration(dMaaPConfiguration.toString()))
            .mapFailure(enhanceError("Could not save new DMaaP configuration to path '%s'", dMaaPConfigPath))
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
        propertiesConfiguration.load(propertiesPath.toFile());
        return propertiesConfiguration;
    }

    private CheckedRunnable saveProperties(Map<String, String> properties) {
        return () -> {
            PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(propertiesPath.toFile());
            propertiesConfiguration.setEncoding(null);
            for (Tuple2<String, String> property : properties) {
                propertiesConfiguration.addProperty(property._1, property._2);
            }
            propertiesConfiguration.save();
        };
    }

    private String indentConfiguration(String configuration) {
        return new JSONObject(configuration).toString(4);
    }

}
