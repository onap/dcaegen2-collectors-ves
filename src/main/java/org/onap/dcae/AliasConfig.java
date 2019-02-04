/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AliasConfig {

  private static final Logger log = LoggerFactory.getLogger(AliasConfig.class);

  private ApplicationSettings applicationSettings;

  @Autowired
  public AliasConfig(ApplicationSettings applicationSettings) {
    this.applicationSettings = applicationSettings;
  }

  public void updateKeystoreAlias(){
    try {
      Enumeration<String> enumeration = getKeystoreAliases(getKeyStore());

      if(enumeration.hasMoreElements()){
        applicationSettings.saveProperty("collector.keystore.alias",enumeration.nextElement());
      }

    } catch (Exception ex) {
      log.error("Cannot update property cause: ", ex);
    }
  }

  private InputStream getKeyStore() throws FileNotFoundException {
    return new FileInputStream(new File(applicationSettings.keystoreFileLocation()));
  }

  private Enumeration<String> getKeystoreAliases(InputStream is) throws Exception {
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(is, new String(getTrustStorePassword(), "UTF-8").toCharArray());
    return keystore.aliases();
  }

  private byte[] getTrustStorePassword() throws IOException {
    return Files.readAllBytes(Paths.get(applicationSettings.truststorePasswordFileLocation()));
  }
}
