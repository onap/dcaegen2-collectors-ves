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
package org.onap.dcae.configuration.cbs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CbsClientFactoryTest {

    @Test
    public void createsClientSuccessfully() {
        // when
        CbsConfigResolver cbsConfigResolver = new CbsConfigResolverFactory().createCbsClient();

        // then
        assertThat(cbsConfigResolver).isNotNull();
    }
}