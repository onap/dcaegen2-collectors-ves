/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights reserved.
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

package org.onap.dcae.common.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.GenericFilterBean;

@Configuration
public class CustomFilter extends GenericFilterBean  {

  private static final String CERTIFICATE_X_509 = "javax.servlet.request.X509Certificate";
  private static final String PROPERTY = "etc/cert_regexp.properties";
  public static final String MESSAGE = "IssuerDN didn't match with any regexp from %s file like %s";

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    String issuerDN = getSubjectDN((X509Certificate[]) servletRequest.getAttribute(CERTIFICATE_X_509));

    Stream<String> lines = Files.lines(Paths.get(PROPERTY));

    if(lines.anyMatch(element -> Pattern.compile(element).matcher(issuerDN).find())){
        filterChain.doFilter(servletRequest,servletResponse);
    }else{
      HttpServletResponse response=(HttpServletResponse) servletResponse;
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      servletResponse.getWriter().write(String.format(
          MESSAGE, PROPERTY,Files.lines(Paths.get(PROPERTY)).collect(Collectors.joining())));
    }
  }

  private String getSubjectDN(X509Certificate[] certs) {
    return Arrays.stream(certs).map(e -> e.getSubjectDN().getName())
        .map(x -> x.split(",")).flatMap(e -> Arrays.stream(e))
        .collect(Collectors.toList()).stream()
        .collect(Collectors.joining(","));
  }
}
