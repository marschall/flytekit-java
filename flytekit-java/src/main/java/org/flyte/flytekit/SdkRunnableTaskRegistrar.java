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

import com.google.auto.service.AutoService;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.flyte.api.v1.Literal;
import org.flyte.api.v1.RunnableTask;
import org.flyte.api.v1.RunnableTaskRegistrar;
import org.flyte.api.v1.TypedInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A registrar that creates {@link SdkRunnableTask} instances. */
@AutoService(RunnableTaskRegistrar.class)
public class SdkRunnableTaskRegistrar extends RunnableTaskRegistrar {
  private static final Logger LOG = LoggerFactory.getLogger(SdkRunnableTaskRegistrar.class);

  private static class RunnableTaskImpl<InputT, OutputT> implements RunnableTask {
    private final SdkRunnableTask<InputT, OutputT> sdkTask;

    private RunnableTaskImpl(SdkRunnableTask<InputT, OutputT> sdkTask) {
      this.sdkTask = sdkTask;
    }

    @Override
    public TypedInterface interface_() {
      return TypedInterface.create(
          sdkTask.getInputType().getVariableMap(), sdkTask.getOutputType().getVariableMap());
    }

    @Override
    public Map<String, Literal> run(Map<String, Literal> inputs) {
      InputT value = sdkTask.getInputType().fromLiteralMap(inputs);
      OutputT output = sdkTask.run(value);

      return sdkTask.getOutputType().toLiteralMap(output);
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Map<String, RunnableTask> load(ClassLoader classLoader) {
    ServiceLoader<SdkRunnableTask> loader = ServiceLoader.load(SdkRunnableTask.class, classLoader);

    LOG.debug("Discovering SdkRunnableTask");

    Map<String, RunnableTask> tasks = new HashMap<>();

    for (SdkRunnableTask<?, ?> sdkTask : loader) {
      String name = sdkTask.getName();

      LOG.debug("Discovered [{}]", name);

      RunnableTask task = new RunnableTaskImpl<>(sdkTask);
      RunnableTask previous = tasks.put(name, task);

      if (previous != null) {
        throw new IllegalArgumentException(
            String.format("Discovered a duplicate task [%s] [%s] [%s]", name, task, previous));
      }
    }

    return tasks;
  }
}