/*-
 * ============LICENSE_START=======================================================
 * dcaegen2-collector-ves
 * ================================================================================
 * Copyright (C) 2023 AT&T Intellectual Property. All rights reserved.
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


package org.onap.dcae.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.configuration.ConfigUpdater;
import org.onap.dcae.restapi.ApiException.ExceptionType;
import org.onap.dcae.common.ConfigProcessors;

@RunWith(MockitoJUnitRunner.class)
public class ConfigProcessorTest {


    @Spy
    ConfigProcessors configProcessors = new ConfigProcessors(new JSONObject());

    @Test
    public void verifyRenameArrayInArray() {

        JSONObject jsonObj = new JSONObject(" {\r\n"
                + "          \"field\": \"event.measurementsForVfScalingFields\",\r\n"
                + "          \"oldField\": \"event.measurementFields\",\r\n"
                + "          \"mapType\": \"renameObject\"\r\n"
                + "        }");
        doReturn("ObjectNotFound").when(configProcessors).getEventObjectVal(Mockito.any());
        configProcessors.renameArrayInArray(jsonObj);
        JSONObject jsonObj1 = new JSONObject(" {\r\n"
                + "          \"field\": \"event[].measurementsForVfScalingFields[]\",\r\n"
                + "          \"oldField\": \"event[].measurementFields[]\",\r\n"
                + "          \"mapType\": \"renameObject\"\r\n"
                + "        }");
        String oldvalue =  "[{\"cpuUsageNice\":0,\"percentUsage\":0.39,\"cpuIdentifier\":\"all\",\"cpuIdle\":99.61,\"cpuUsageSystem\":0,\"cpuUsageUser\":0.22},{\"cpuUsageNice\":0,\"percentUsage\":0.36,\"cpuIdentifier\":\"cpu0\",\"cpuIdle\":99.64,\"cpuUsageSystem\":0,\"cpuUsageUser\":0.21},{\"cpuUsageNice\":0,\"percentUsage\":0.33,\"cpuIdentifier\":\"cpu3\",\"cpuIdle\":99.67,\"cpuUsageSystem\":0,\"cpuUsageUser\":0.19}]";
        doReturn(oldvalue).when(configProcessors).getEventObjectVal(Mockito.any());
        configProcessors.renameArrayInArray(jsonObj1);

    }

    @Test
    public void verifyCheckFilter() {

        //Test for key as matches
        JSONObject jsonObj = new JSONObject("{\r\n"
                + "        \"event.commonEventHeader.reportingEntityName\": \"matches:.*ircc|irpr.*\"\r\n"
                + "      }");

        //Should return true when pattern not matched and logickey is "not"
        doReturn("random").when(configProcessors).getEventObjectVal(Mockito.any());
        assertTrue((configProcessors.checkFilter(jsonObj, "event.commonEventHeader.reportingEntityName", "not")));

        //Should return false when pattern matched and logickey is "not"
        doReturn("ircc").when(configProcessors).getEventObjectVal(Mockito.any());
        assertFalse((configProcessors.checkFilter(jsonObj, "event.commonEventHeader.reportingEntityName", "not")));

        //Should return false when pattern not matched
        doReturn("random").when(configProcessors).getEventObjectVal(Mockito.any());
        assertFalse((configProcessors.checkFilter(jsonObj, "event.commonEventHeader.reportingEntityName", "")));

        //Test for key as contains
        JSONObject jsonObj1 = new JSONObject("{\r\n"
                + "        \"event.commonEventHeader.reportingEntityName\": \"contains:ircc\"\r\n"
                + "      }");

        //Should return false when pattern matched and logickey is "not"
        doReturn("vnfircc001").when(configProcessors).getEventObjectVal(Mockito.any());
        assertFalse((configProcessors.checkFilter(jsonObj1, "event.commonEventHeader.reportingEntityName", "not")));


        doReturn("random").when(configProcessors).getEventObjectVal(Mockito.any());
        assertFalse((configProcessors.checkFilter(jsonObj1, "event.commonEventHeader.reportingEntityName", "")));

        //Test for  value as string
        JSONObject jsonObj3 = new JSONObject("{\r\n"
                + "        \"event.commonEventHeader.reportingEntityName\": \"testabc\"\r\n"
                + "      }");

        //Should return false when pattern matched and logickey is "not"
        doReturn("testabc").when(configProcessors).getEventObjectVal(Mockito.any());
        assertFalse((configProcessors.checkFilter(jsonObj3, "event.commonEventHeader.reportingEntityName", "not")));

        //Should return false when pattern not matched
        doReturn("False").when(configProcessors).getEventObjectVal(Mockito.any());
        assertFalse((configProcessors.checkFilter(jsonObj3, "event.commonEventHeader.reportingEntityName", "")));

    }

    @Test
    public void verifyIsFilterMet() {
        JSONObject jsonObj = new JSONObject("{\r\n"
                + "      \"event.commonEventHeader.domain\": \"measurementsForVfScaling\",\r\n"
                + "      \"VESversion\": \"v4\",\r\n"
                + "      \"not\": {\r\n"
                + "        \"event.commonEventHeader.reportingEntityName\": \"matches:.*ircc|irpr.*\"\r\n"
                + "         }"
                + "      }");
        doReturn(false).when(configProcessors).checkFilter(Mockito.any(),Mockito.any(),Mockito.any());
        assertFalse((configProcessors.isFilterMet(jsonObj))); 

        doReturn(true).when(configProcessors).checkFilter(Mockito.any(),Mockito.any(),Mockito.any());
        assertTrue((configProcessors.isFilterMet(jsonObj)));
    }

    @Test
    public void verifyConvertMBtoKBOperation() {
        String operation = "convertMBtoKB";
        String expval = "5120.0";
        assertEquals (expval, configProcessors.performOperation( operation, "5"));
    }
}
