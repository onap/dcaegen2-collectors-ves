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
package org.onap.dcae.vestest;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import org.junit.Test;
import org.onap.dcae.commonFunction.DmaapPropertyReader;

public class TestDmaapPropertyReader {

    @Test
    public void shouldReadDMaaPHashes() {
        DmaapPropertyReader dmaapPropertyReader = new DmaapPropertyReader("src/test/resources/testDmaapConfig_ip.json");
        HashMap<String, String> dmaapHash = dmaapPropertyReader.dmaap_hash;
        assertEquals(dmaapHash.get("sec_fault_ueb.cambria.hosts"),
            "uebsb91kcdc.it.att.com,uebsb92kcdc.it.att.com,uebsb93kcdc.it.att.com");
        assertEquals(dmaapHash.get("sec_fault_ueb.cambria.topic"), "DCAE-SE-COLLECTOR-EVENTS-DEV");
    }

    @Test
    public void shouldReadDMaaPHashesForSecondGeneration() {
        DmaapPropertyReader dmaapPropertyReader = new DmaapPropertyReader(
            "src/test/resources/testDmaapConfig_gen2.json");
        HashMap<String, String> dmaapHash = dmaapPropertyReader.dmaap_hash;
        assertEquals(dmaapHash.get("ves-thresholdCrossingAlert-secondary.cambria.topic"),
            "DCAE-SE-COLLECTOR-EVENTS-DEV");
        assertEquals(dmaapHash.get("ves-thresholdCrossingAlert-secondary.cambria.url"), "UEBHOST:3904");
        assertEquals(dmaapHash.get("ves-fault-secondary.cambria.url"), "UEBHOST:3904");
        assertEquals(dmaapHash.get("ves-fault-secondary.cambria.topic"), "DCAE-SE-COLLECTOR-EVENTS-DEV");
    }

}

