/*
 * Copyright 2012-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.json;

import com.facebook.buck.event.BuckEventBus;
import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.rules.Description;
import com.facebook.buck.util.Console;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class DefaultProjectBuildFileParserFactory implements ProjectBuildFileParserFactory {
  private final ProjectFilesystem projectFilesystem;
  private final String pythonInterpreter;
  private final boolean allowEmptyGlobs;
  private final ImmutableSet<Description<?>> descriptions;

  public DefaultProjectBuildFileParserFactory(
      ProjectFilesystem projectFilesystem,
      String pythonInterpreter,
      boolean allowEmptyGlobs,
      ImmutableSet<Description<?>> descriptions) {
    this.projectFilesystem = projectFilesystem;
    this.pythonInterpreter = pythonInterpreter;
    this.allowEmptyGlobs = allowEmptyGlobs;
    this.descriptions = descriptions;
  }

  @Override
  public ProjectBuildFileParser createParser(
      Iterable<String> commonIncludes,
      Console console,
      ImmutableMap<String, String> environment,
      BuckEventBus buckEventBus) {
    return new ProjectBuildFileParser(
        projectFilesystem,
        commonIncludes,
        pythonInterpreter,
        allowEmptyGlobs,
        descriptions,
        console,
        environment,
        buckEventBus);
  }
}
