/*
 * Copyright (C) 2016 Selerity, Inc. (support@seleritycorp.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seleritycorp.rds.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import static org.easymock.EasyMock.expect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.seleritycorp.common.base.config.ApplicationPaths;
import com.seleritycorp.common.base.config.Config;
import com.seleritycorp.common.base.test.FileTestCase;
import com.seleritycorp.common.base.test.SettableConfig;

public class RdsDataPersisterTest extends FileTestCase {
  ApplicationPaths paths;
  Config config;
  Path defaultTarget;
  Path defaultTmpTarget;

  Path tmpDir;

  @Before
  public void setUp() throws IOException {
    tmpDir = createTempDirectory();

    paths = createMock(ApplicationPaths.class);
    expect(paths.getDataPath()).andReturn(tmpDir);

    config = new SettableConfig();

    defaultTarget = tmpDir.resolve("rds").resolve("rds-data.json");
    defaultTmpTarget = tmpDir.resolve("rds").resolve("rds-data.json.tmp");
  }

  @Test
  public void testPersistOk() throws IOException {
    JsonArray rdsData = new JsonArray();
    rdsData.add(42);
    rdsData.add("foo");

    replayAll();

    RdsDataPersister persister = createRdsDataPersister();
    persister.persist(rdsData);

    verifyAll();

    assertThat(defaultTarget).hasContent("[42,\"foo\"]");
    assertThat(defaultTmpTarget).doesNotExist();
  }

  @Test
  public void testPersistTmpTargetParentIsFile() throws IOException {
    JsonArray rdsData = new JsonArray();

    Files.createFile(defaultTmpTarget.getParent());

    replayAll();

    RdsDataPersister persister = createRdsDataPersister();

    try {
      persister.persist(rdsData);
      failBecauseExceptionWasNotThrown(IOException.class);
    } catch (IOException e) {
      assertThat(e).hasMessageContaining(defaultTmpTarget.getParent().toString());
    }

    verifyAll();
  }

  @Test
  public void testPersistTmpTargetIsDirectory() throws IOException {
    JsonArray rdsData = new JsonArray();

    Files.createDirectories(defaultTmpTarget.getParent());
    Files.createDirectories(defaultTmpTarget);

    replayAll();

    RdsDataPersister persister = createRdsDataPersister();

    try {
      persister.persist(rdsData);
      failBecauseExceptionWasNotThrown(IOException.class);
    } catch (IOException e) {
      assertThat(e).hasMessageContaining(defaultTmpTarget.toString());
    }

    verifyAll();
  }

  @Test
  public void testPersistTargetParentIsFile() throws IOException {
    JsonArray rdsData = new JsonArray();

    Files.createFile(defaultTarget.getParent());

    replayAll();

    RdsDataPersister persister = createRdsDataPersister();

    try {
      persister.persist(rdsData);
      failBecauseExceptionWasNotThrown(IOException.class);
    } catch (IOException e) {
      assertThat(e).hasMessageContaining(defaultTarget.getParent().toString());
    }

    verifyAll();
  }

  @Test
  public void testPersistTargetIsDirectory() throws IOException {
    JsonArray rdsData = new JsonArray();

    Files.createDirectories(defaultTarget.getParent());
    Files.createDirectories(defaultTarget);

    replayAll();

    RdsDataPersister persister = createRdsDataPersister();

    try {
      persister.persist(rdsData);
      failBecauseExceptionWasNotThrown(IOException.class);
    } catch (IOException e) {
      assertThat(e).hasMessageContaining(defaultTarget.toString());
    }

    verifyAll();
  }

  @Test
  public void testPersistOverwriting() throws IOException {
    JsonArray rdsData = new JsonArray();

    Files.createDirectories(defaultTmpTarget.getParent());
    Files.write(defaultTmpTarget, "foo".getBytes(StandardCharsets.UTF_8));
    try {
      Files.createDirectories(defaultTarget.getParent());
    } catch (FileAlreadyExistsException e) {
      // defaultTmpTarget and defaultTarget may share the parent, so the directory may already
      // exist.
    }
    Files.write(defaultTmpTarget, "bar".getBytes(StandardCharsets.UTF_8));

    replayAll();

    RdsDataPersister persister = createRdsDataPersister();

    persister.persist(rdsData);

    verifyAll();

    assertThat(defaultTarget).hasContent("[]");
    assertThat(defaultTmpTarget).doesNotExist();
  }

  private RdsDataPersister createRdsDataPersister() {
    return new RdsDataPersister(config, paths);
  }
}
