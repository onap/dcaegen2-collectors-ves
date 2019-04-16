/*
 * Copyright (C) 2019 VMware, Inc. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onap.dcae.restapi;

import static org.junit.Assert.assertTrue;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.LinkedBlockingQueue;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.onap.dcae.ApplicationSettings;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * @author nil
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes=VesRestController.class)
class VesRestControllerTest {

  @Autowired
  private VesRestController vesController;
  
  @MockBean(name="applicationSettings")
  private ApplicationSettings applicationSettings;
  
  @MockBean(name="inputQueue")
  private LinkedBlockingQueue<JSONObject> inputQueue;
  
  @MockBean(name="incomingRequestsLogger")
  private Logger incomingRequestsLogger;
  
  @MockBean(name="errorLog")
  private Logger errorLog;
  
  @MockBean(name="metricsLog")
  private Logger metricsLog;
  
  private HttpServletRequest createRequest() throws URISyntaxException {
    return MockMvcRequestBuilders.post(new URI("/eventListener/v4")).header("X-MinorVersion", "8").buildRequest(null);
  }

  @Test
  void testWrongMinorVer() {
    try {
      ResponseEntity<String> rsp = vesController.receiveEvent("{\"key\":\"name\"}", createRequest());

      // "eventListener": ["4.7.2","5.4.1","7.0.1"]
      HttpHeaders rspHeaders = rsp.getHeaders();
      assertTrue(rspHeaders.getFirst("X-LatestVersion").equals("7.0.1"));
      assertTrue(rspHeaders.getFirst("X-MinorVersion").equals("7"));
      assertTrue(rspHeaders.getFirst("X-PatchVersion").equals("2"));
    } catch (Exception e) {
      assertTrue(false);
    }
  }
}
