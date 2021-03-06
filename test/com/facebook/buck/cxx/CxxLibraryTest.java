/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.buck.cxx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.android.AndroidPackageable;
import com.facebook.buck.android.AndroidPackageableCollector;
import com.facebook.buck.cli.FakeBuckConfig;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargetFactory;
import com.facebook.buck.python.PythonPackageComponents;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleParamsFactory;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.BuildTargetSourcePath;
import com.facebook.buck.rules.FakeBuildRule;
import com.facebook.buck.rules.PathSourcePath;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.step.Step;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CxxLibraryTest {

  @Test
  public void cxxLibraryInterfaces() {
    SourcePathResolver pathResolver = new SourcePathResolver(new BuildRuleResolver());
    BuildTarget target = BuildTargetFactory.newInstance("//foo:bar");
    BuildRuleParams params = BuildRuleParamsFactory.createTrivialBuildRuleParams(target);
    CxxPlatform cxxPlatform = new DefaultCxxPlatform(new FakeBuckConfig());

    // Setup some dummy values for the header info.
    final BuildTarget headerTarget = BuildTargetFactory.newInstance("//:header");
    final BuildTarget headerSymlinkTreeTarget = BuildTargetFactory.newInstance("//:symlink");
    final Path headerSymlinkTreeRoot = Paths.get("symlink/tree/root");

    // Setup some dummy values for the library archive info.
    final BuildRule archive = new FakeBuildRule("//:archive", pathResolver);
    final Path archiveOutput = Paths.get("output/path/lib.a");

    // Setup some dummy values for the library archive info.
    final BuildRule sharedLibrary = new FakeBuildRule("//:shared", pathResolver);
    final Path sharedLibraryOutput = Paths.get("output/path/lib.so");
    final String sharedLibrarySoname = "lib.so";

    // Construct a CxxLibrary object to test.
    AbstractCxxLibrary cxxLibrary = new AbstractCxxLibrary(params, pathResolver) {

      @Override
      public CxxPreprocessorInput getCxxPreprocessorInput(CxxPlatform cxxPlatform) {
        return CxxPreprocessorInput.builder()
            .setRules(ImmutableSet.of(headerTarget, headerSymlinkTreeTarget))
            .setIncludeRoots(headerSymlinkTreeRoot)
            .build();
      }

      @Override
      public NativeLinkableInput getNativeLinkableInput(CxxPlatform cxxPlatform, Type type) {
        return type == Type.STATIC ?
            new NativeLinkableInput(
                ImmutableList.<SourcePath>of(new BuildTargetSourcePath(archive.getBuildTarget())),
                ImmutableList.of(archiveOutput.toString())) :
            new NativeLinkableInput(
                ImmutableList.<SourcePath>of(
                    new BuildTargetSourcePath(sharedLibrary.getBuildTarget())),
                ImmutableList.of(sharedLibraryOutput.toString()));
      }

      @Override
      public PythonPackageComponents getPythonPackageComponents(CxxPlatform cxxPlatform) {
        return new PythonPackageComponents(
            ImmutableMap.<Path, SourcePath>of(),
            ImmutableMap.<Path, SourcePath>of(),
            ImmutableMap.<Path, SourcePath>of(
                Paths.get(sharedLibrarySoname),
                new PathSourcePath(sharedLibraryOutput)));
      }

      @Override
      public Iterable<AndroidPackageable> getRequiredPackageables() {
        return ImmutableList.of();
      }

      @Override
      public void addToCollector(AndroidPackageableCollector collector) {}

      @Override
      public ImmutableMap<String, SourcePath> getSharedLibraries(CxxPlatform cxxPlatform) {
        return ImmutableMap.of();
      }

    };

    // Verify that we get the header/symlink targets and root via the CxxPreprocessorDep
    // interface.
    CxxPreprocessorInput expectedCxxPreprocessorInput = CxxPreprocessorInput.builder()
        .setRules(ImmutableSet.of(headerTarget, headerSymlinkTreeTarget))
        .setIncludeRoots(headerSymlinkTreeRoot)
        .build();
    assertEquals(expectedCxxPreprocessorInput, cxxLibrary.getCxxPreprocessorInput(cxxPlatform));

    // Verify that we get the static archive and it's build target via the NativeLinkable
    // interface.
    NativeLinkableInput expectedStaticNativeLinkableInput = new NativeLinkableInput(
        ImmutableList.<SourcePath>of(new BuildTargetSourcePath(archive.getBuildTarget())),
        ImmutableList.of(archiveOutput.toString()));
    assertEquals(
        expectedStaticNativeLinkableInput,
        cxxLibrary.getNativeLinkableInput(
            cxxPlatform,
            NativeLinkable.Type.STATIC));

    // Verify that we get the static archive and it's build target via the NativeLinkable
    // interface.
    NativeLinkableInput expectedSharedNativeLinkableInput = new NativeLinkableInput(
        ImmutableList.<SourcePath>of(new BuildTargetSourcePath(sharedLibrary.getBuildTarget())),
        ImmutableList.of(sharedLibraryOutput.toString()));
    assertEquals(
        expectedSharedNativeLinkableInput,
        cxxLibrary.getNativeLinkableInput(
            cxxPlatform,
            NativeLinkable.Type.SHARED));

    // Verify that we return the expected output for python packages.
    PythonPackageComponents expectedPythonPackageComponents = new PythonPackageComponents(
        ImmutableMap.<Path, SourcePath>of(),
        ImmutableMap.<Path, SourcePath>of(),
        ImmutableMap.<Path, SourcePath>of(
            Paths.get(sharedLibrarySoname),
            new PathSourcePath(sharedLibraryOutput)));
    assertEquals(
        expectedPythonPackageComponents,
        cxxLibrary.getPythonPackageComponents(cxxPlatform));

    // Verify that the implemented BuildRule methods are effectively unused.
    assertEquals(ImmutableList.<Step>of(), cxxLibrary.getBuildSteps(null, null));
    assertNull(cxxLibrary.getPathToOutputFile());
    assertTrue(ImmutableList.copyOf(cxxLibrary.getInputs()).isEmpty());
  }

}
