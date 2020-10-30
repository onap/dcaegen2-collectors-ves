/*-
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.configuration.ConfigurationHandler;
import reactor.core.Disposable;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationConfigProviderTest {

    @Mock
    private ConfigurationHandler configurationHandler;

    @Mock
    private Disposable disposable;

    @InjectMocks
    @Spy
    private ApplicationConfigProvider applicationConfigProvider;

    @Test
    public void shouldStopJobAndCloseConnectionWhenErrorOccurredDuringListenAtConsulChange() {

       // given
       Mockito.doThrow(new RuntimeException("Simulate exception")).when(configurationHandler).startListen(any());

       // when
       applicationConfigProvider.run();

       // then
        Mockito.verify(applicationConfigProvider).closeConnection(any());
    }

    @Test
    public void shouldSendReloadAction() {

        // when
        applicationConfigProvider.reload(Duration.ofMillis(1));

        // then
        Mockito.verify(applicationConfigProvider).sendReloadAction();
    }
}
