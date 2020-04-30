/*
 * Copyright 2020 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.flyte.flytekit;

import java.util.List;
import java.util.Map;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableList;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableMap;
import org.flyte.api.v1.Binding;
import org.flyte.api.v1.Node;
import org.flyte.api.v1.PartialTaskIdentifier;
import org.flyte.api.v1.TaskNode;
import org.flyte.api.v1.Variable;

/** Represent a {@link org.flyte.flytekit.SdkRunnableTask} in a workflow DAG. */
public class SdkTaskNode extends SdkNode {
  private final String nodeId;
  private final PartialTaskIdentifier taskId;
  private final Map<String, SdkBindingData> inputs;
  private final Map<String, Variable> outputs;

  SdkTaskNode(
      SdkWorkflowBuilder builder,
      String nodeId,
      PartialTaskIdentifier taskId,
      Map<String, SdkBindingData> inputs,
      Map<String, Variable> outputs) {
    super(builder);

    this.nodeId = nodeId;
    this.taskId = taskId;
    this.inputs = inputs;
    this.outputs = outputs;
  }

  @Override
  public Map<String, SdkBindingData> getOutputs() {
    return this.outputs.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Map.Entry::getKey,
                entry -> SdkBindingData.ofOutputReference(nodeId, entry.getKey())));
  }

  @Override
  public String getNodeId() {
    return nodeId;
  }

  @Override
  public Node toIdl() {
    TaskNode taskNode = TaskNode.create(taskId);

    List<Binding> bindings =
        inputs.entrySet().stream()
            .map(x -> Binding.create(x.getKey(), x.getValue().toIdl()))
            .collect(ImmutableList.toImmutableList());

    return Node.builder().id(nodeId).taskNode(taskNode).inputs(bindings).build();
  }
}