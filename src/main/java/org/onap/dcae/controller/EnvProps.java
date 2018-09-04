/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
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

import java.util.Objects;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
final class EnvProps {

    final String consulProtocol;
    final String consulHost;
    final int consulPort;
    final String cbsName;
    final String cbsProtocol;
    final String appName;

    EnvProps(String consulProtocol, String consulHost, int consulPort, String cbsProtocol, String cbsName, String appName) {
        this.consulProtocol = consulProtocol;
        this.consulHost = consulHost;
        this.consulPort = consulPort;
        this.cbsProtocol = cbsProtocol;
        this.cbsName = cbsName;
        this.appName = appName;
    }

    @Override
    public String toString() {
        return "EnvProps{" +
            "consulProtocol='" + consulProtocol + '\'' +
            ", consulHost='" + consulHost + '\'' +
            ", consulPort=" + consulPort +
            ", cbsProtocol='" + cbsProtocol + '\'' +
            ", cbsName='" + cbsName + '\'' +
            ", appName='" + appName + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvProps envProps = (EnvProps) o;
        return consulPort == envProps.consulPort &&
            Objects.equals(consulProtocol, envProps.consulProtocol) &&
            Objects.equals(consulHost, envProps.consulHost) &&
            Objects.equals(cbsProtocol, envProps.cbsProtocol) &&
            Objects.equals(cbsName, envProps.cbsName) &&
            Objects.equals(appName, envProps.appName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consulProtocol, consulHost, consulPort, cbsProtocol, cbsName, appName);
    }
}