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

package com.facebook.buck.rules;

import com.facebook.buck.parser.NoSuchBuildTargetException;

/**
 * Takes in an {@link TargetNode} from the target graph and builds a {@link BuildRule}.
 */
public class TargetNodeToBuildRuleTransformer {

  public <T> BuildRule transform(
      TargetGraph targetGraph,
      BuildRuleResolver ruleResolver,
      TargetNode<T> targetNode)
      throws NoSuchBuildTargetException {
    BuildRuleFactoryParams ruleFactoryParams = targetNode.getRuleFactoryParams();
    Description<T> description = targetNode.getDescription();

    T arg = targetNode.getConstructorArg();

    // The params used for the Buildable only contain the declared parameters. However, the deps of
    // the rule include not only those, but also any that were picked up through the deps declared
    // via a SourcePath.
    BuildRuleParams params = new BuildRuleParams(
        targetNode.getBuildTarget(),
        ruleResolver.getAllRules(targetNode.getDeclaredDeps()),
        ruleResolver.getAllRules(targetNode.getExtraDeps()),
        ruleFactoryParams.getProjectFilesystem(),
        ruleFactoryParams.getRuleKeyBuilderFactory(),
        description.getBuildRuleType(),
        targetGraph);
    BuildRule buildRule = description.createBuildRule(params, ruleResolver, arg);

    // Note that describedRule has not been added to the BuildRuleResolver yet.
    if (description instanceof FlavorableDescription && !targetNode.getBuildTarget().isFlavored()) {
      FlavorableDescription<T> flavorable = (FlavorableDescription<T>) description;
      flavorable.registerFlavors(
          arg,
          buildRule,
          ruleFactoryParams.getProjectFilesystem(),
          ruleFactoryParams.getRuleKeyBuilderFactory(),
          targetGraph,
          ruleResolver);
    }

    return buildRule;
  }
}
