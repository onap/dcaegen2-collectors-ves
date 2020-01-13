/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2019 VMware, Inc. All rights reserved.
 * Copyright (C) 2019 Nokia. All rights reserved.s
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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author nil
 */
@Component
public class HeaderUtils {

  public String getApiVerFilePath(String fileName) {
    return Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource(fileName))
        .getPath();
  }

  public String getRestApiIdentify(String uri) {
    return isBatchRequest(uri) ? "eventListener_eventBatch" : "eventListener";
  }

  public Map<String, String> extractHeaders(HttpServletRequest request) {
    return Collections.list(request.getHeaderNames()).stream()
        .collect(Collectors.toMap(h -> h, request::getHeader));
  }

  public HttpHeaders fillHeaders(Map<String, String> headers) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setAll(headers);
    return httpHeaders;
  }

  private boolean isBatchRequest(String request) {
    return request.contains("eventBatch");
  }
}
