/*
 * Copyright 2014-2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.iep.http;

import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;

@RunWith(JUnit4.class)
public class ClientConfigTest {

  private final SettableConfig archaius = new DefaultSettableConfig();

  private void clear(String k) {
    archaius.clearProperty(k);
  }

  private void set(String k, String v) {
    archaius.setProperty(k, v);
  }

  private ClientConfig cfg;

  @Before
  public void before() {
    clear("foo.niws.client.UseIpAddress");
    clear("niws.client.UseIpAddress");
    final URI uri = URI.create("/test");
    cfg = new ClientConfig(archaius, "foo", "foo:7001", uri, uri, null);
  }

  @Test
  public void useIpAddressNoProp() {
    Assert.assertEquals(cfg.useIpAddress(), true);
  }

  @Test
  public void useIpAddressDefaultProp() {
    set("niws.client.UseIpAddress", "false");
    Assert.assertEquals(cfg.useIpAddress(), false);
  }

  @Test
  public void useIpAddressNamedProp() {
    set("foo.niws.client.UseIpAddress", "false");
    Assert.assertEquals(cfg.useIpAddress(), false);
  }

  @Test
  public void useIpAddressNamedOverridesDefaultProp() {
    set("niws.client.UseIpAddress", "true");
    set("foo.niws.client.UseIpAddress", "false");
    Assert.assertEquals(cfg.useIpAddress(), false);
  }

  @Test
  public void fromUriNiws() {
    ClientConfig config = ClientConfig.fromUri(archaius, URI.create("niws://foo/bar"), null);
    Assert.assertEquals(config.uri().toString(), "/bar");
  }

  @Test
  public void fromUriNiwsWithQuery() {
    ClientConfig config = ClientConfig.fromUri(archaius, URI.create("niws://foo/bar?a=b"), null);
    Assert.assertEquals(config.uri().toString(), "/bar?a=b");
  }

  @Test
  public void fromUriNiwsWithAbsolute() {
    ClientConfig config = ClientConfig.fromUri(archaius, URI.create("niws://foo/http://foo.com/bar"), null);
    Assert.assertEquals(config.name(), "foo");
    Assert.assertEquals(config.originalUri().toString(), "niws://foo/http://foo.com/bar");
    Assert.assertEquals(config.uri().toString(), "http://foo.com/bar");
    Assert.assertEquals(config.relativeUri(), "/bar");
  }

  @Test
  public void fromUriVip() {
    ClientConfig config = ClientConfig.fromUri(archaius, URI.create("vip://foo:vip:7001/bar"), null);
    Assert.assertEquals(config.uri().toString(), "/bar");
    Assert.assertEquals(config.vip(), "vip:7001");
  }

  @Test
  public void fromUriVipWithQuery() {
    ClientConfig config = ClientConfig.fromUri(archaius, URI.create("vip://foo:vip:7001/bar?a=b"), null);
    Assert.assertEquals(config.uri().toString(), "/bar?a=b");
    Assert.assertEquals(config.vip(), "vip:7001");
  }

  @Test
  public void doubleSlash() {
    URI niws = URI.create("niws://some-vip//api/v2/update");
    ClientConfig cfg = ClientConfig.fromUri(archaius, niws, null);
    Assert.assertEquals("/api/v2/update", cfg.relativeUri());

    URI query = URI.create("niws://some-vip//api/v2/update?foo=//");
    ClientConfig cfg1 = ClientConfig.fromUri(archaius, query, null);
    Assert.assertEquals("/api/v2/update?foo=//", cfg1.relativeUri());

    URI simple = URI.create("http://some-vip//api/v2/update?foo=//");
    ClientConfig cfg2 = ClientConfig.fromUri(archaius, simple, null);
    Assert.assertEquals("/api/v2/update?foo=//", cfg2.relativeUri());
  }

  // TODO: does ClientConfig need a proper equals? Looks like this test case just accidentally
  // worked in earlier versions of equalsverifier.
  @Ignore
  public void equalsContract() {
    EqualsVerifier.forClass(ClientConfig.class).usingGetClass().verify();
  }
}
