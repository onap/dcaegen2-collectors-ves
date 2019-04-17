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
import org.onap.dcae.ApplicationException;
import org.onap.dcae.ApplicationSettings;

public class SubjectMatcher {

  private final ApplicationSettings properties;
  private final X509Certificate[] cert;

  public SubjectMatcher(ApplicationSettings properties, X509Certificate[] cert) {
    this.properties = properties;
    this.cert = cert;
  }

  public boolean match(){
    try {
      return getLines().anyMatch(element -> Pattern.compile(element).matcher(getSubjectDN(cert)).find());
    } catch (IOException e) {
      throw new ApplicationException("Cannot read file cause: "+ e);
    }
  }

  public Stream<String> getLines() throws IOException {
    return Files.lines(Paths.get(properties.certSubjectMatcher()));
  }

  public String getSubjectDN(X509Certificate[] certs) {
    return Arrays.stream(certs).map(e -> e.getSubjectDN().getName())
        .map(x -> x.split(",")).flatMap(Arrays::stream)
        .collect(Collectors.joining(","));
  }

  public boolean isCert() {
    return cert !=null;
  }
}
