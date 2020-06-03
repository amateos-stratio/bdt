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
import com.stratio.qa.models.marathon.AppResponse;
import com.stratio.qa.models.marathon.MarathonConstants;
import com.stratio.qa.models.marathon.Task;
import com.stratio.qa.utils.ThreadProperty;

import static org.assertj.core.api.Assertions.assertThat;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class MarathonSpec extends BaseGSpec {

    private final Logger logger = LoggerFactory.getLogger(CCTSpec.class);

    private MarathonApiClient marathonApiClient;

    public MarathonSpec(CommonG spec) {
        this.commonspec = spec;
        marathonApiClient = MarathonApiClient.getInstance(this.commonspec);
    }

    @Then("^service with id '(.*)' has '(\\d+)' task[s]? in '(running|finished|failed|staging|starting|killed)' state in Marathon$")
    public void checkNumberOfTasksState(String appId, int numberOfTasks, String state) throws Exception {
        AppResponse app = marathonApiClient.getApp(appId);
        Collection<Task> tasks = app.getApp().getTasks();

        String translatedState = MarathonConstants.statesDict.get(state);
        int count = (int) tasks.stream().filter(task -> task.getState().equals(translatedState)).count();

        assertThat(count)
                .as("Number of task in state " + translatedState + " for service " + appId + " does not match.")
                .isEqualTo(numberOfTasks);
    }

    @Then("^service with id '(.*)' has all tasks in '(running|finished|failed|staging|starting|killed)' state in Marathon$")
    public void checkAllTasksState(String appId, String state) throws Exception {
        AppResponse app = marathonApiClient.getApp(appId);
        Collection<Task> tasks = app.getApp().getTasks();

        String translatedState = MarathonConstants.statesDict.get(state);
        int count = (int) tasks.stream().filter(task -> task.getState().equals(translatedState)).count();

        assertThat(count)
                .as("Number of task in state " + translatedState + " for service " + appId + " does not match.")
                .isEqualTo(tasks.size());
    }

    @Then("^I get environment variable '(.*)' for service with id '(.*)' and save the value in environment variable '(.+?)'$")
    public void saveServiceEnvVariable(String serviceEnvVar, String serviceId, String envVar) throws Exception {
        AppResponse app = marathonApiClient.getApp(serviceId);
        assertThat(app.getApp().getEnv().get(serviceEnvVar))
                .as("Environment variable " + serviceEnvVar + " not found for service " + serviceId)
                .isNotNull();

        String value = app.getApp().getEnv().get(serviceEnvVar).toString();
        ThreadProperty.set(envVar, value);
    }

    @Then("^I get label '(.*)' for service with id '(.*)' and save the value in environment variable '(.+?)'$")
    public void saveServiceLabel(String label, String serviceId, String envVar) throws Exception {
        AppResponse app = marathonApiClient.getApp(serviceId);
        assertThat(app.getApp().getLabels().get(label))
                .as("Label " + label + " not found for service " + serviceId)
                .isNotNull();

        String value = app.getApp().getLabels().get(label);
        ThreadProperty.set(envVar, value);
    }

    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, service with id '(.*)' has '(\\d+)' task[s]? in '(running|finished|failed|staging|starting|killed)' state in Marathon$")
    public void checkNumberOfTasksStateWithPolling(int timeout, int pause, String appId, int numberOfTasks, String state) throws Exception {
        AppResponse app;
        Collection<Task> tasks;
        int count;
        String translatedState = MarathonConstants.statesDict.get(state);

        int time = 0;
        while (time < timeout) {
            app = marathonApiClient.getApp(appId);
            if (app.getApp() != null) {
                tasks = app.getApp().getTasks();

                count = (int) tasks.stream().filter(task -> task.getState().equals(translatedState)).count();
                if (count == numberOfTasks) {
                    return;
                }
            }

            Thread.sleep(pause * 1000);
            time += pause;
        }

        assertThat(false)
                .as("Number of task in state " + translatedState + " for service " + appId + " does not match after " + timeout + " seconds.")
                .isTrue();
    }

    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, service with id '(.*)' has all tasks in '(running|finished|failed|staging|starting|killed)' state in Marathon$")
    public void checkAllTasksStateWithPolling(int timeout, int pause, String appId, String state) throws Exception {
        AppResponse app;
        Collection<Task> tasks;

        String translatedState = MarathonConstants.statesDict.get(state);
        int count;

        int time = 0;
        while (time < timeout) {
            app = marathonApiClient.getApp(appId);
            if (app.getApp() != null) {
                tasks = app.getApp().getTasks();

                count = (int) tasks.stream().filter(task -> task.getState().equals(translatedState)).count();
                if (count == tasks.size()) {
                    return;
                }
            }

            Thread.sleep(pause * 1000);
            time += pause;
        }

        assertThat(false)
                .as("Number of task in state " + translatedState + " for service " + appId + " does not match after " + timeout + " seconds.")
                .isTrue();
    }

    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, service with id '(.*)' has '(\\d+)' '(healthy|unhealthy|staged|running)' task[s]? in Marathon$")
    public void checkNumberOfTasksHealthinessWithPolling(int timeout, int pause, String appId, int numberOfTasks, String state) throws Exception {
        AppResponse app;
        int count = 0;

        int time = 0;
        while (time < timeout) {
            app = marathonApiClient.getApp(appId);

            if (app.getApp() != null) {
                switch (state) {
                    case "healthy":
                        count = app.getApp().getTasksHealthy();
                        break;
                    case "unhealthy":
                        count = app.getApp().getTasksUnhealthy();
                        break;
                    case "staged":
                        count = app.getApp().getTasksStaged();
                        break;
                    case "running":
                        count = app.getApp().getTasksRunning();
                        break;
                    default:
                        count = 0;
                }

                if (count == numberOfTasks) {
                    return;
                }
            }

            Thread.sleep(pause * 1000);
            time += pause;
        }

        assertThat(false)
                .as("Number of task(s) " + state + " for service " + appId + " does not match after " + timeout + " seconds.")
                .isTrue();
    }

    @Then("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, service with id '(.*)' has all tasks '(healthy|unhealthy|unknown)' in Marathon$")
    public void checkAllTasksHealthinessWithPolling(int timeout, int pause, String appId, String state) throws Exception {
        AppResponse app;
        int count = 0;

        int time = 0;
        while (time < timeout) {
            app = marathonApiClient.getApp(appId);
            if (app.getApp() != null) {
                switch (state) {
                    case "healthy":
                        count = app.getApp().getTasksHealthy();
                        break;
                    case "unhealthy":
                        count = app.getApp().getTasksUnhealthy();
                        break;
                    case "staged":
                        count = app.getApp().getTasksStaged();
                        break;
                    case "running":
                        count = app.getApp().getTasksRunning();
                        break;
                    default:
                        count = 0;
                }

                if (count == app.getApp().getTasks().size()) {
                    return;
                }
            }

            Thread.sleep(pause * 1000);
            time += pause;
        }

        assertThat(false)
                .as("Number of task(s) " + state + " for service " + appId + " does not match after " + timeout + " seconds.")
                .isTrue();
    }

    @When("^I get taskId for task '(.+?)' in service with id '(.+?)' from Marathon and save the value in environment variable '(.+?)'$")
    public void getTaskId(String taskName, String serviceId, String envVar) throws Exception {
        String taskId = MarathonApiClient.utils.getTaskId(taskName, serviceId);
        ThreadProperty.set(envVar, taskId);
    }
}
