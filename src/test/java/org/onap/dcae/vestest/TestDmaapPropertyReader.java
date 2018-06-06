package org.onap.dcae.vestest;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import org.junit.Test;
import org.onap.dcae.commonFunction.DmaapPropertyReader;

public class TestDmaapPropertyReader {

    @Test
    public void shouldReadDMaaPHashes() {
        DmaapPropertyReader dmaapPropertyReader = new DmaapPropertyReader("src/test/resources/sampleDMaapConfig.json");
        HashMap<String, String> dmaapHash = dmaapPropertyReader.dmaap_hash;
        assertEquals(dmaapHash.get("sec_fault_ueb.cambria.hosts"),
            "uebsb91kcdc.it.att.com,uebsb92kcdc.it.att.com,uebsb93kcdc.it.att.com");
        assertEquals(dmaapHash.get("sec_fault_ueb.cambria.topic"), "DCAE-SE-COLLECTOR-EVENTS-DEV");
    }

    @Test
    public void shouldReadDMaaPHashesForSecondGeneration() {
        DmaapPropertyReader dmaapPropertyReader = new DmaapPropertyReader(
            "src/test/resources/sampleDMaaPConfig_2ndGeneration.json");
        HashMap<String, String> dmaapHash = dmaapPropertyReader.dmaap_hash;
        assertEquals(dmaapHash.get("ves-thresholdCrossingAlert-secondary.cambria.topic"),
            "DCAE-SE-COLLECTOR-EVENTS-DEV");
        assertEquals(dmaapHash.get("ves-thresholdCrossingAlert-secondary.cambria.url"), "UEBHOST:3904");
        assertEquals(dmaapHash.get("ves-fault-secondary.cambria.url"), "UEBHOST:3904");
        assertEquals(dmaapHash.get("ves-fault-secondary.cambria.topic"), "DCAE-SE-COLLECTOR-EVENTS-DEV");
    }

}

