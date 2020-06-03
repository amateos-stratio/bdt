/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.qa.specs;

import com.stratio.qa.clients.marathon.MarathonApiClient;
import com.stratio.qa.clients.mesos.MesosApiClient;
import com.stratio.qa.models.mesos.MesosConstants;
import com.stratio.qa.models.mesos.MesosTask;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MesosSpec extends BaseGSpec {

    private MesosApiClient mesosApiClient;

    public MesosSpec(CommonG spec) {

        this.commonspec = spec;
        mesosApiClient = MesosApiClient.getInstance(this.commonspec);
    }

    @Then("^task with id '(.+?)'( does not)? exist[s]? in mesos$")
    public void checkTaskId(String taskId, String notExist) throws Exception {
        if (notExist == null) {
            assertThat(mesosApiClient.getMesosTask(taskId).getTasks().size())
                            .as("Mesos task for id " + taskId + " not found in mesos").isNotEqualTo(0);
        } else {
            assertThat(mesosApiClient.getMesosTask(taskId).getTasks().size())
                            .as("Mesos task for id " + taskId + " found in mesos").isEqualTo(0);
        }
    }

    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, task with id '(.+?)'( does not)? exist[s]? in mesos$")
    public void checkTaskIdWithPolling(int timeout, int pause, String taskId, String notExist) throws Exception {
        int time = 0;
        while (time < timeout) {

            if (mesosApiClient.getMesosTask(taskId).getTasks().size() == 0 && notExist != null) {
                return;
            } else if (mesosApiClient.getMesosTask(taskId).getTasks().size() != 0 && notExist == null) {
                return;
            }

            Thread.sleep(pause * 1000);
            time += pause;
        }
        String message = notExist == null ? "not " : "";
        assertThat(false)
                        .as("Mesos task for id " + taskId + " " + message + "found in mesos after " + timeout + " seconds").isTrue();
    }

    @Then("^task with id '(.+?)' appears with state '(running|killed|failed|finished|staging|starting)' in mesos$")
    public void checkTaskIdState(String taskId, String state) throws Exception {

        List<MesosTask> tasks = mesosApiClient.getMesosTask(taskId).getTasks();
        assertThat(tasks.size() > 0)
                .as("Mesos task for id " + taskId + " found in mesos")
                .isTrue();
        assertThat(tasks.get(0).getState().equals(MesosConstants.statesDict.get(state)))
                .as("Mesos task for id " + taskId + " found in mesos with state " + state)
                .isTrue();
    }

    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, task with id '(.+?)' appears with state '(running|killed|failed|finished|staging|starting)' in mesos$")
    public void checkTaskIdStateWithPolling(int timeout, int pause, String taskId, String state) throws Exception {
        int time = 0;
        List<MesosTask> tasks;
        while (time < timeout) {

            tasks = mesosApiClient.getMesosTask(taskId).getTasks();
            if (tasks.size() != 0 && tasks.get(0).getState().equals(MesosConstants.statesDict.get(state))) {
                return;
            }

            Thread.sleep(pause * 1000);
            time += pause;
        }

        assertThat(false)
                .as("Mesos task for id " + taskId + " found in mesos with state " + state + " after " + timeout + " seconds").isTrue();
    }

    @When("^I get container name for task '(.+?)' in service with id '(.+?)' from Marathon and save the value in environment variable '(.+?)'$")
    public void getMesosTaskContainerName(String taskName, String serviceId, String envVar) throws Exception {
        String taskId = MarathonApiClient.utils.getTaskId(taskName, serviceId);

        MesosTask mesosTask = mesosApiClient.getMesosTask(taskId).getTasks().get(0);
        String containerId = MesosApiClient.utils.getMesosTaskContainerId(mesosTask);
        assertThat(containerId).as("Error searching containerId for mesos task: " + taskId).isNotNull();

        String containerName = "mesos-".concat(containerId);
        ThreadProperty.set(envVar, containerName);
    }
}
