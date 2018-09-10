//
//        ================================================================================
//        Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
//        Copyright (c) 2018 Nokia. All rights reserved.
//        ================================================================================
//        Licensed under the Apache License, Version 2.0 (the "License");
//        you may not use this file except in compliance with the License.
//        You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//        Unless required by applicable law or agreed to in writing, software
//        distributed under the License is distributed on an "AS IS" BASIS,
//        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//        See the License for the specific language governing permissions and
//        limitations under the License.
//        ============LICENSE_END=========================================================
//
//
package org.onap.dcae.commonFunction;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigProcessorAdapterTest {

    @Mock
    private ConfigProcessors configProcessors;

    @InjectMocks
    private EventProcessor.ConfigProcessorAdapter configProcessorAdapter;


    @Test
    public void shouldCallIsFilterMetOnAdapter() {
        //given
        JSONObject parameter = new JSONObject();
        when(configProcessors.isFilterMet(parameter)).thenReturn(true);
        //when
        boolean actualReturn = configProcessorAdapter.isFilterMet(parameter);
        //then
        assertTrue(actualReturn);
        verify(configProcessors, times(1)).isFilterMet(parameter);
    }

    @Test
    public void shouldCallGivenMethodFromConfigProcessor() throws Exception {
        JSONObject parameter = new JSONObject();
        String exampleFunction = "concatenateValue";
        //when
        configProcessorAdapter.runConfigProcessorFunctionByName(exampleFunction, parameter);
        //then
        verify(configProcessors, times(1)).concatenateValue(parameter);
    }

}