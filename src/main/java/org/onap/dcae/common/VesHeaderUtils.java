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
package org.onap.dcae.common;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.HttpHeaders;

/**
 * @author nil
 *
 */
public final class VesHeaderUtils {

  /**
   * constructor
   */
  private VesHeaderUtils() {

  }

  /**
   * Judge whether request is eventBatch
   * 
   * @param request request uri
   * @return true when request is eventBatch
   */
  public static boolean isBatchRequest(String request) {
    return request.contains("eventBatch");
  }

  /**
   * get api version file path
   * 
   * @param fileName version file name
   * @return file path
   */
  public static String getApiVerFilePath(String fileName) {
    return ClassLoader.getSystemClassLoader().getResource(fileName).getPath();
  }

  /**
   * get restful api identify
   * 
   * @param reqUri rest request uri
   * @return api identify
   */
  public static String getRestApiIdentify(String reqUri) {
    return isBatchRequest(reqUri) ? "eventListener_eventBatch" : "eventListener";
  }

  /**
   * get request headers in map format
   * 
   * @param httpReq restful request
   * @return headers
   */
  public static Map<String, String> getReqHeaderMap(HttpServletRequest httpReq) {
    Map<String, String> headerMap = new HashMap();

    String currName;
    Enumeration<String> headerNames = httpReq.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      currName = headerNames.nextElement();
      headerMap.put(currName, httpReq.getHeader(currName));

    }

    return headerMap;
  }

  /**
   * fill response http header with header Map
   * 
   * @param rspHeaderMap response headers in Map format
   * @return HttpHeader
   */
  public static HttpHeaders fillRspHttpHeaders(Map<String, String> rspHeaderMap) {
    HttpHeaders rspHeaders = new HttpHeaders();
    if (MapUtils.isEmpty(rspHeaderMap)) {
      return rspHeaders;
    }
    rspHeaderMap.forEach((k,v) -> rspHeaders.add(k,v));
    return rspHeaders;
  }
}
