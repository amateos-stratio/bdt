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

import com.ning.http.client.Response;
import com.stratio.qa.assertions.Assertions;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Generic API Rest Specs.
 *
 * @see <a href="RestSpec-annotations.html">REST Steps &amp; Matching Regex</a>
 */
public class RestSpec extends BaseGSpec {

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public RestSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * Send requests to {@code restHost @code restPort}.
     *
     * @param restHost host where api is running
     * @param restPort port where api is running
     */
    @Given("^I( securely)? send requests to '([^:]+?)(:.+?)?'$")
    public void setupRestClient(String isSecured, String restHost, String restPort) {
        String restProtocol = "http://";

        if (isSecured != null) {
            restProtocol = "https://";
        }


        if (restHost == null) {
            restHost = "localhost";
        }

        if (restPort == null) {
            if (isSecured == null) {
                restPort = ":80";
            } else {
                restPort = ":443";
            }
        }

        commonspec.setRestProtocol(restProtocol);
        commonspec.setRestHost(restHost);
        commonspec.setRestPort(restPort);
    }

    /**
     * Send a request of the type specified but in this case, the response is checked until it contains the expected value
     *
     * @param requestType   type of request to be sent. Possible values:
     *                      GET|DELETE|POST|PUT|CONNECT|PATCH|HEAD|OPTIONS|REQUEST|TRACE
     * @param timeout
     * @param wait
     * @param responseVal
     * @param endPoint      end point to be used
     * @param baseData      path to file containing the schema to be used
     * @param type          element to read from file (element should contain a json)
     * @param modifications DataTable containing the modifications to be done to the
     *                      base schema element. Syntax will be:
     *                      {@code
     *                      | <key path> | <type of modification> | <new value> |
     *                      }
     *                      where:
     *                      key path: path to the key to be modified
     *                      type of modification: DELETE|ADD|UPDATE
     *                      new value: in case of UPDATE or ADD, new value to be used
     *                      for example:
     *                      if the element read is {"key1": "value1", "key2": {"key3": "value3"}}
     *                      and we want to modify the value in "key3" with "new value3"
     *                      the modification will be:
     *                      | key2.key3 | UPDATE | "new value3" |
     *                      being the result of the modification: {"key1": "value1", "key2": {"key3": "new value3"}}
     * @throws Exception
     */
    @Given("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, I send a '(.+?)' request to '(.+?)' so that the response( does not)? contains '(.+?)' based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void sendRequestDataTableTimeout(Integer timeout, Integer wait, String requestType, String endPoint, String contains, String responseVal, String baseData, String type, DataTable modifications) throws Exception {
        // Retrieve data
        String retrievedData = commonspec.retrieveData(baseData, type);

        // Modify data
        commonspec.getLogger().debug("Modifying data {} as {}", retrievedData, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications);

        Boolean searchUntilContains;
        if (contains == null || contains.isEmpty()) {
            searchUntilContains = Boolean.TRUE;
        } else {
            searchUntilContains = Boolean.FALSE;
        }
        Boolean found = !searchUntilContains;
        AssertionError ex = null;

        Future<Response> response;

        Pattern pattern = CommonG.matchesOrContains(responseVal);

        for (int i = 0; (i <= timeout); i += wait) {
            if (found && searchUntilContains) {
                break;
            }
            try {
                commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", requestType, endPoint, modifiedData, type);
                response = commonspec.generateRequest(requestType, false, null, null, endPoint, modifiedData, type);
                commonspec.getLogger().debug("Saving response");
                commonspec.setResponse(requestType, response.get());
                commonspec.getLogger().debug("Checking response value");

                if (searchUntilContains) {
                    assertThat(commonspec.getResponse().getResponse()).containsPattern(pattern);
                    found = true;
                    timeout = i;
                } else {
                    assertThat(commonspec.getResponse().getResponse()).doesNotContain(responseVal);
                    found = false;
                    timeout = i;
                }
            } catch (AssertionError | Exception e) {
                if (!found) {
                    commonspec.getLogger().info("Response value not found after " + i + " seconds");
                } else {
                    commonspec.getLogger().info("Response value found after " + i + " seconds");
                }
                Thread.sleep(wait * 1000);
                if (e instanceof AssertionError) {
                    ex = (AssertionError) e;
                }
            }
            if (!found && !searchUntilContains) {
                break;
            }
        }
        if ((!found && searchUntilContains) || (found && !searchUntilContains)) {
            throw (ex);
        }
        if (searchUntilContains) {
            commonspec.getLogger().info("Success! Response value found after " + timeout + " seconds");
        } else {
            commonspec.getLogger().info("Success! Response value not found after " + timeout + " seconds");
        }
    }

    /**
     * Send a request of the type specified
     *
     * @param requestType   type of request to be sent. Possible values:
     *                      GET|DELETE|POST|PUT|CONNECT|PATCH|HEAD|OPTIONS|REQUEST|TRACE
     * @param endPoint      end point to be used
     * @param baseData      path to file containing the schema to be used
     * @param type          element to read from file (element should contain a json)
     * @param modifications DataTable containing the modifications to be done to the
     *                      base schema element. Syntax will be:
     *                      {@code
     *                      | <key path> | <type of modification> | <new value> |
     *                      }
     *                      where:
     *                      key path: path to the key to be modified
     *                      type of modification: DELETE|ADD|UPDATE
     *                      new value: in case of UPDATE or ADD, new value to be used
     *                      for example:
     *                      if the element read is {"key1": "value1", "key2": {"key3": "value3"}}
     *                      and we want to modify the value in "key3" with "new value3"
     *                      the modification will be:
     *                      | key2.key3 | UPDATE | "new value3" |
     *                      being the result of the modification: {"key1": "value1", "key2": {"key3": "new value3"}}
     * @throws Exception
     */
    @When("^I send a '(.+?)' request to '(.+?)'( with user and password '(.+:.+?)')? based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void sendRequest(String requestType, String endPoint, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        // Retrieve data
        String retrievedData = commonspec.retrieveData(baseData, type);

        // Modify data
        commonspec.getLogger().debug("Modifying data {} as {}", retrievedData, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications);

        String user = null;
        String password = null;
        if (loginInfo != null) {
            user = loginInfo.substring(0, loginInfo.indexOf(':'));
            password = loginInfo.substring(loginInfo.indexOf(':') + 1);
        }


        commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", requestType, endPoint, modifiedData, type);
        Future<Response> response = commonspec.generateRequest(requestType, false, user, password, endPoint, modifiedData, type, "");

        // Save response
        commonspec.getLogger().debug("Saving response");
        commonspec.setResponse(requestType, response.get());
    }


    @When("^I create '(policy|user|group)' '(.+?)' using API service path '(.+?)'( with user and password '(.+:.+?)')? based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createResource(String resource, String resourceId, String endPoint, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        createResourceIfNotExist(resource, resourceId, endPoint, loginInfo, false, baseData, type, modifications);

    }


    @When("^I create '(policy|user|group)' '(.+?)' using API service path '(.+?)'( with user and password '(.+:.+?)')? if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createResourceIfNotExist(String resource, String resourceId, String endPoint, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        createResourceIfNotExist(resource, resourceId, endPoint, loginInfo, true, baseData, type, modifications);
    }


    /**
     * Creates a custom resource in gosec management if the resource doesn't exist
     *
     * @param resource
     * @param resourceId    (userId, groupId or policyId)
     * @param endPoint
     * @param loginInfo
     * @param doesNotExist  (if 'empty', creation is forced deleting the previous policy if exists)
     * @param baseData
     * @param type
     * @param modifications
     * @throws Exception
     */
    private void createResourceIfNotExist(String resource, String resourceId, String endPoint, String loginInfo, boolean doesNotExist, String baseData, String type, DataTable modifications) throws Exception {
        Integer expectedStatusCreate = 201;
        Integer expectedStatusDelete = 200;
        String endPointResource = endPoint + resourceId;
        String endPointPolicy = "/service/gosecmanagement/api/policy";
        String endPointPolicies = "/service/gosecmanagement/api/policies";
        String newEndPoint = "";

        if (endPoint.contains("id")) {
            newEndPoint = endPoint.replace("?id=", "");
        } else {
            newEndPoint = endPoint.substring(0, endPoint.length() - 1);
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                sendRequestNoDataTable("GET", endPointPolicy, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                    String policyId = commonspec.getCommandResult().trim();
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = newEndPoint + "/" + policyId;
                    } else {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                        policyId = commonspec.getCommandResult().trim();
                        if (!policyId.equals("")) {
                            commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                            endPointResource = newEndPoint + "/" + policyId;
                        }
                    }
                } else {
                    if (commonspec.getResponse().getStatusCode() == 404) {
                        commonspec.getLogger().warn("Error 404 accessing endpoint {}: checking the new endpoint for Gosec 1.1.1", endPointPolicy);
                        sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                        if (commonspec.getResponse().getStatusCode() == 200) {
                            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                            String policyId = commonspec.getCommandResult().trim();
                            if (!policyId.equals("")) {
                                commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                                endPointResource = newEndPoint + "?id=" + policyId;
                            } else {
                                commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                                policyId = commonspec.getCommandResult().trim();
                                if (!policyId.equals("")) {
                                    commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                                    endPointResource = newEndPoint + "?id=" + policyId;
                                }
                            }
                        }
                    }
                }
            }

            sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {
                sendRequest("POST", newEndPoint, loginInfo, baseData, type, modifications);
                try {
                    if (commonspec.getResponse().getStatusCode() == 409) {
                        commonspec.getLogger().warn("The resource {} already exists", resourceId);
                    } else {
                        assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                        commonspec.getLogger().debug("Resource {} created", resourceId);
                    }
                } catch (Exception e) {
                    commonspec.getLogger().warn("Error creating user {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().warn("{}:{} already exist", resource, resourceId);
                if (resource.equals("policy") && commonspec.getResponse().getStatusCode() == 200) {
                    if (doesNotExist) {
                        //Policy already exists
                        commonspec.getLogger().warn("Policy {} already exist - not created", resourceId);

                    } else {
                        //Delete policy if exists
                        sendRequest("DELETE", endPointResource, loginInfo, baseData, type, modifications);
                        commonspec.getLogger().warn("Policy {} deleted", resourceId);

                        try {
                            assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusDelete);
                        } catch (Exception e) {
                            commonspec.getLogger().warn("Error deleting Policy {}: {}", resourceId, commonspec.getResponse().getResponse());
                            throw e;
                        }
                        createResourceIfNotExist(resource, resourceId, endPoint, loginInfo, doesNotExist, baseData, type, modifications);
                    }
                }
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    /**
     * Deletes a resource in gosec management if the resourceId exists previously.
     *
     * @param resource
     * @param resourceId
     * @param endPoint
     * @param loginInfo
     * @throws Exception
     */
    @When("^I delete '(policy|user|group)' '(.+?)' using API service path '(.+?)'( with user and password '(.+:.+?)')? if it exists$")
    public void deleteUserIfExists(String resource, String resourceId, String endPoint, String loginInfo) throws Exception {
        Integer[] expectedStatusDelete = {200, 204};
        String endPointResource = endPoint + resourceId;
        String endPointPolicy = "/service/gosecmanagement/api/policy";
        String endPointPolicies = "/service/gosecmanagement/api/policies";

        if (endPoint.contains("id")) {
            endPoint = endPoint.replace("?id=", "");
        } else {
            endPoint = endPoint.substring(0, endPoint.length() - 1);
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                sendRequestNoDataTable("GET", endPointPolicy, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                    String policyId = commonspec.getCommandResult().trim();
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPoint + "/" + policyId;
                    } else {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                        policyId = commonspec.getCommandResult().trim();
                        if (!policyId.equals("")) {
                            commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                            endPointResource = endPoint + "/" + policyId;
                        }
                    }
                } else {
                    if (commonspec.getResponse().getStatusCode() == 404) {
                        commonspec.getLogger().warn("Error 404 accessing endpoint {}: checking the new endpoint for Gosec 1.1.1", endPointPolicy);
                        sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                        if (commonspec.getResponse().getStatusCode() == 200) {
                            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                            String policyId = commonspec.getCommandResult().trim();
                            if (!policyId.equals("")) {
                                commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                                endPointResource = endPoint + "?id=" + policyId;
                            } else {
                                commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                                policyId = commonspec.getCommandResult().trim();
                                if (!policyId.equals("")) {
                                    commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                                    endPointResource = endPoint + "?id=" + policyId;
                                }
                            }
                        }
                    }
                }
            }

            sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                //Delete user if exists
                sendRequestNoDataTable("DELETE", endPointResource, loginInfo, null, null);
                commonspec.getLogger().debug("Resource {} deleted", resourceId);

                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                } catch (Exception e) {
                    commonspec.getLogger().warn("Error deleting Resource {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().debug("Resource {} with id {} not found so it's not deleted", resource, resourceId);
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}: {}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    /**
     * Same sendRequest, but in this case, we do not receive a data table with modifications.
     * Besides, the data and request header are optional as well.
     * In case we want to simulate sending a json request with empty data, we just to avoid baseData
     *
     * @param requestType
     * @param endPoint
     * @param baseData
     * @param type
     * @throws Exception
     */
    @When("^I send a '(.+?)' request to '(.+?)'( with user and password '(.+:.+?)')?( based on '([^:]+?)')?( as '(json|string|gov)')?$")
    public void sendRequestNoDataTable(String requestType, String endPoint, String loginInfo, String baseData, String type) throws Exception {
        Future<Response> response;
        String user = null;
        String password = null;

        if (loginInfo != null) {
            user = loginInfo.substring(0, loginInfo.indexOf(':'));
            password = loginInfo.substring(loginInfo.indexOf(':') + 1);
        }

        if (baseData != null) {
            // Retrieve data
            String retrievedData = commonspec.retrieveData(baseData, type);
            // Generate request
            response = commonspec.generateRequest(requestType, false, user, password, endPoint, retrievedData, type, "");
        } else {
            // Generate request
            response = commonspec.generateRequest(requestType, false, user, password, endPoint, "", type, "");
        }

        // Save response
        commonspec.setResponse(requestType, response.get());
    }


    /**
     * Same sendRequest, but in this case, the response is checked until it contains the expected value
     *
     * @param timeout
     * @param wait
     * @param requestType
     * @param endPoint
     * @param responseVal
     * @throws Exception
     */
    @When("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, I send a '(.+?)' request to '(.+?)' so that the response( does not)? contains '(.+?)'$")
    public void sendRequestTimeout(Integer timeout, Integer wait, String requestType, String endPoint, String contains, String responseVal) throws Exception {
        AssertionError ex = null;
        String type = "";
        Future<Response> response;

        if (responseVal != null) {
            Boolean searchUntilContains;
            if (contains == null || contains.isEmpty()) {
                searchUntilContains = Boolean.TRUE;
            } else {
                searchUntilContains = Boolean.FALSE;
            }
            Boolean found = !searchUntilContains;

            Pattern pattern = CommonG.matchesOrContains(responseVal);
            for (int i = 0; (i <= timeout); i += wait) {
                if (found && searchUntilContains) {
                    break;
                }
                try {
                    response = commonspec.generateRequest(requestType, false, null, null, endPoint, "", type, "");
                    commonspec.setResponse(requestType, response.get());
                    commonspec.getLogger().debug("Checking response value");

                    if (searchUntilContains) {
                        assertThat(commonspec.getResponse().getResponse()).containsPattern(pattern);
                        found = true;
                        timeout = i;
                    } else {
                        assertThat(commonspec.getResponse().getResponse()).doesNotContain(responseVal);
                        found = false;
                        timeout = i;
                    }
                } catch (AssertionError | Exception e) {
                    if (!found) {
                        commonspec.getLogger().info("Response value not found after " + i + " seconds");
                    } else {
                        commonspec.getLogger().info("Response value found after " + i + " seconds");
                    }
                    Thread.sleep(wait * 1000);
                    if (e instanceof AssertionError) {
                        ex = (AssertionError) e;
                    }
                }
                if (!found && !searchUntilContains) {
                    break;
                }
            }
            if ((!found && searchUntilContains) || (found && !searchUntilContains)) {
                throw (ex);
            }
            if (searchUntilContains) {
                commonspec.getLogger().info("Success! Response value found after " + timeout + " seconds");
            } else {
                commonspec.getLogger().info("Success! Response value not found after " + timeout + " seconds");
            }
        } else {

            for (int i = 0; (i <= timeout); i += wait) {
                try {
                    response = commonspec.generateRequest(requestType, false, null, null, endPoint, "", type, "");
                    commonspec.setResponse(requestType, response.get());
                    commonspec.getLogger().debug("Checking response value");

                    assertThat(commonspec.getResponse().getResponse());
                    timeout = i;
                } catch (AssertionError | Exception e) {
                    Thread.sleep(wait * 1000);
                    if (e instanceof AssertionError) {
                        ex = (AssertionError) e;
                    }
                }
            }
        }
    }

    @When("^I login to '(.+?)' based on '([^:]+?)' as '(json|string)'$")
    public void loginUser(String endPoint, String baseData, String type) throws Exception {
        sendRequestNoDataTable("POST", endPoint, null, baseData, type);
    }

    @When("^I login to '(.+?)' based on '([^:]+?)' as '(json|string)' with:$")
    public void loginUser(String endPoint, String baseData, String type, DataTable modifications) throws Exception {
        sendRequest("POST", endPoint, null, baseData, type, modifications);
    }

    @When("^I logout from '(.+?)'$")
    public void logoutUser(String endPoint) throws Exception {
        sendRequestNoDataTable("GET", endPoint, null, "", "");
    }

    @Then("^the service response must contain the text '(.*?)'$")
    public void assertResponseMessage(String expectedText) throws SecurityException, IllegalArgumentException {
        Pattern pattern = CommonG.matchesOrContains(expectedText);
        assertThat(commonspec.getResponse().getResponse()).containsPattern(pattern);
    }

    @Then("^the service response must not contain the text '(.*?)'$")
    public void assertNegativeResponseMessage(String expectedText) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        assertThat(commonspec.getResponse().getResponse()).doesNotContain(expectedText);
    }

    @Then("^the service response status must be '(\\d+)'( and its response length must be '(\\d+)')?( and its response must contain the text '(.*?)')?$")
    public void assertResponseStatusLength(Integer expectedStatus, String sExpectedLength, String expectedText) {
        Integer expectedLength = sExpectedLength != null ? Integer.parseInt(sExpectedLength) : null;
        if (expectedLength != null || expectedText != null) {
            if (expectedLength != null) {
                assertThat(Optional.of(commonspec.getResponse())).hasValueSatisfying(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(expectedStatus);
                    assertThat((new JSONArray(r.getResponse())).length()).isEqualTo(expectedLength);
                });
            }
            if (expectedText != null) {
                Pattern pattern = CommonG.matchesOrContains(expectedText);
                assertThat(Optional.of(commonspec.getResponse())).hasValueSatisfying(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(expectedStatus);
                    assertThat(r.getResponse()).containsPattern(pattern);
                });
            }
        } else {
            try {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatus);
            } catch (AssertionError e) {
                commonspec.getLogger().warn("Response: {}", commonspec.getResponse().getResponse());
                throw e;
            }
        }
    }

    @Then("^I save service response( in environment variable '(.*?)')?( in file '(.*?)')?$")
    public void saveResponseInEnvironmentVariableFile(String envVar, String fileName) throws Exception {

        if (envVar != null || fileName != null) {
            String value = commonspec.getResponse().getResponse();

            if (envVar != null) {
                ThreadProperty.set(envVar, value);
            }

            if (fileName != null) {
                // Create file (temporary) and set path to be accessible within test
                File tempDirectory = new File(System.getProperty("user.dir") + "/target/test-classes/");
                String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
                commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
                // Note that this Writer will delete the file if it exists
                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), StandardCharsets.UTF_8));
                try {
                    out.write(value);
                } catch (Exception e) {
                    commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
                } finally {
                    out.close();
                }

                Assertions.assertThat(new File(absolutePathFile).isFile());
            }
        } else {
            fail("No environment variable neither file defined");
        }
    }

    @When("^I get id from( tag)? policy with name '(.+?)' and save it in environment variable '(.+?)'$")
    public void getPolicyId(String tag, String policyName, String envVar) throws Exception {
        String endPoint = "/service/gosecmanagement/api/policy";
        String newEndPoint = "/service/gosecmanagement/api/policies";
        String errorMessage = "api/policies";
        String errorMessage2 = "api/policy";

        if (tag != null) {
            endPoint = "/service/gosecmanagement/api/policy/tag";
            newEndPoint = "/service/gosecmanagement/api/policies/tags";
            errorMessage = "api/policies/tags";
            errorMessage2 = "api/policy/tag";
        }
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
            commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
            if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
                if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                    fail("Error obtaining ID from policy " + policyName);
                }
            }
        } else {
            if (commonspec.getResponse().getStatusCode() == 404) {
                commonspec.getLogger().warn("Error 404 accessing endpoint {}: checking the new endpoint for Gosec 1.1.1", endPoint);
                sendRequestNoDataTable("GET", newEndPoint, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                    commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
                    if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                        commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
                        if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                            fail("Error obtaining ID from policy " + policyName);
                        }
                    }
                } else {
                    fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", errorMessage);
                }
            } else {
                fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", errorMessage2);
            }
        }
    }

    @When("^I create tenant '(.+?)' if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createTenant(String tenantId, String baseData, String type, DataTable modifications) throws Exception {
        String endPoint = "/service/gosec-identities-daas/identities/tenants";
        String endPointResource = endPoint + "/" + tenantId;
        Integer expectedStatus = 201;
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        sendRequestNoDataTable("GET", endPointResource, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.getLogger().warn("Tenant {} already exist - not created", tenantId);
        } else {
            sendRequest("POST", endPoint, null, baseData, type, modifications);
            try {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatus);
            } catch (Exception e) {
                commonspec.getLogger().warn("Error creating Tenant {}: {}", tenantId, commonspec.getResponse().getResponse());
                throw e;
            }
        }
    }

    @When("^I include '(user|group)' '(.+?)' in tenant '(.+?)'$")
    public void includeResourceInTenant(String resource, String resourceId, String tenantId) throws Exception {
        String endPointGetAllUsers = "/service/gosec-identities-daas/identities/users";
        String endPointGetAllGroups = "/service/gosec-identities-daas/identities/groups";
        String endPointTenant = "/service/gosec-identities-daas/identities/tenants/" + tenantId;
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        String uidOrGid = "uid";
        String uidOrGidTenant = "uids";
        String endPointGosec = endPointGetAllUsers;
        if (resource.equals("group")) {
            uidOrGid = "gid";
            uidOrGidTenant = "gids";
            endPointGosec = endPointGetAllGroups;
        }
        sendRequestNoDataTable("GET", endPointGosec, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            if (commonspec.getResponse().getResponse().contains("\"" + uidOrGid + "\":\"" + resourceId + "\"")) {
                sendRequestNoDataTable("GET", endPointTenant, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    JsonObject jsonTenantInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                    if (((JsonArray) jsonTenantInfo.get(uidOrGidTenant)).values().contains(JsonValue.valueOf(resourceId))) {
                        commonspec.getLogger().debug("{} is already included in tenant", resourceId);
                    } else {
                        ((JsonArray) jsonTenantInfo.get(uidOrGidTenant)).add(resourceId);
                        Future<Response> response = commonspec.generateRequest("PATCH", false, null, null, endPointTenant, JsonValue.readHjson(jsonTenantInfo.toString()).toString(), "json", "");
                        commonspec.setResponse("PATCH", response.get());
                        if (commonspec.getResponse().getStatusCode() != 204) {
                            throw new Exception("Error adding " + resource + " " + resourceId + " in tenant " + tenantId + " - Status code: " + commonspec.getResponse().getStatusCode());
                        }
                    }
                } else {
                    throw new Exception("Error obtaining info from tenant " + tenantId + " - Status code: " + commonspec.getResponse().getStatusCode());
                }
            } else {
                throw new Exception(resource + " " + resourceId + " doesn't exist in Gosec");
            }
        } else {
            throw new Exception("Error obtaining " + resource + "s - Status code: " + commonspec.getResponse().getStatusCode());
        }
    }

    @When("^I get id from profile with name '(.+?)' and save it in environment variable '(.+?)'$")
    public void getProfiled(String profileName, String envVar) throws Exception {
        String endPoint = "/service/gosec-identities-daas/identities/profiles";

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + profileName + "\").pid' | sed s/\\\"//g");
            commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
            if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                fail("Error obtaining ID from profile " + profileName);
            }
        } else {
            commonspec.getLogger().warn("Profile with id: {} does not exist", profileName);
        }
    }

    @When("^I get json from( tag)? policy with name '(.+?)' and save it( in environment variable '(.*?)')?( in file '(.*?)')?$")
    public void getPolicyJson(String tag, String policyName, String envVar, String fileName) throws Exception {
        String endPoint = "/service/gosecmanagement/api/policy";
        String newEndPoint = "/service/gosecmanagement/api/policies";
        String errorMessage = "api/policies";
        String errorMessage2 = "api/policy";

        if (tag != null) {
            endPoint = "/service/gosecmanagement/api/policy/tag";
            newEndPoint = "/service/gosecmanagement/api/policies/tags";
            errorMessage = "api/policies/tags";
            errorMessage2 = "api/policy/tag";
        }
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
            if (commonspec.getCommandResult().trim().equals("")) {
                commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
            }
            sendRequestNoDataTable("GET", "/service/gosecmanagement/api/policy/" + commonspec.getCommandResult(), null, null, null);

            if (envVar != null) {
                ThreadProperty.set(envVar, commonspec.getResponse().getResponse());

                if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                    fail("Error obtaining JSON from policy " + policyName);
                }
            }

            if (fileName != null) {
                // Create file (temporary) and set path to be accessible within test
                File tempDirectory = new File(System.getProperty("user.dir") + "/target/test-classes/");
                String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
                commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
                // Note that this Writer will delete the file if it exists
                Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), StandardCharsets.UTF_8));
                try {
                    out.write(commonspec.getResponse().getResponse());
                } catch (Exception e) {
                    commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
                } finally {
                    out.close();
                }

                Assertions.assertThat(new File(absolutePathFile).isFile());
            }

        } else {
            if (commonspec.getResponse().getStatusCode() == 404) {
                commonspec.getLogger().warn("Error 404 accessing endpoint {}: checking the new endpoint for Gosec 1.1.1", endPoint);
                sendRequestNoDataTable("GET", newEndPoint, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                    if (commonspec.getCommandResult().trim().equals("")) {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                    }
                    sendRequestNoDataTable("GET", "/service/gosecmanagement/api/policy?id=" + commonspec.getCommandResult(), null, null, null);

                    if (envVar != null) {
                        ThreadProperty.set(envVar, commonspec.getResponse().getResponse());
                        if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                            fail("Error obtaining JSON from policy " + policyName);
                        }
                    }

                    if (fileName != null) {
                        // Create file (temporary) and set path to be accessible within test
                        File tempDirectory = new File(System.getProperty("user.dir") + "/target/test-classes/");
                        String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
                        commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
                        // Note that this Writer will delete the file if it exists
                        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), StandardCharsets.UTF_8));
                        try {
                            out.write(commonspec.getResponse().getResponse());
                        } catch (Exception e) {
                            commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
                        } finally {
                            out.close();
                        }

                        Assertions.assertThat(new File(absolutePathFile).isFile());
                    }

                } else {
                    fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", errorMessage);
                }
            } else {
                fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", errorMessage2);
            }
        }
    }

    @When("^I( force)? create '(certificate|keytab|password|password_nouser)' '(.+?)' using deploy-api (with|without) parameters( path '(.+?)')?( cn '(.+?)')?( name '(.+?)')?( alt '(.+?)')?( organization '(.+?)')?( principal '(.+?)')?( realm '(.+?)')?( user '(.+?)')?( password '(.+?)')?$")
    public void createSecret(String force, String secretType, String secret, String withOrWithout, String path, String cn, String name, String alt, String organizationName, String principal, String realm, String user, String password) throws Exception {
        String baseUrl = "/service/deploy-api/secrets";
        String secretTypeAux;
        String urlParams;
        switch (secretType) {
            case "certificate":
                urlParams = getCertificateUrlParams(secret, path, cn, name, alt, organizationName);
                secretTypeAux = "certificates";
                break;
            case "keytab":
                urlParams = getKeytabUrlParams(secret, path, name, principal, realm);
                secretTypeAux = "kerberos";
                break;
            case "password":
                urlParams = getPasswordUrlParams(secret, path, name, user, password);
                secretTypeAux = "passwords";
                break;
            default:
                urlParams = "";
                secretTypeAux = "default";
        }
        if (force != null) {
            String pathAux = path != null ? path.replaceAll("/", "%2F") + secret : "%2Fuserland%2F" + secretTypeAux + "%2F" + secret;
            sendRequestNoDataTable("DELETE", baseUrl + "?path=" + pathAux, null, null, null);
        }
        if (!secretType.equals("password_nouser")) {
            sendRequestNoDataTable("POST", baseUrl + "/" + secretType + urlParams, null, null, null);
        } else {
            String pathAux = (path != null ? path.replaceAll("/", "%2F") + secret : "%2Fuserland%2Fpasswords%2F" + secret) + "%2F" + (name != null ? name : secret);
            String filePath = createCustomSecretFile(password != null ? password : secret);
            sendRequestNoDataTable("POST", baseUrl + "/custom?path=" + pathAux, null, filePath, "json");
        }
    }

    private String createCustomSecretFile(String password) throws IOException {
        File tempDirectory = new File(System.getProperty("user.dir") + "/target/test-classes/");
        String fileName = System.currentTimeMillis() + ".json";
        String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
        commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
        // Note that this Writer will delete the file if it exists
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), StandardCharsets.UTF_8));
        try {
            out.write("{\"pass\": \"" + password + "\"}");
        } catch (Exception e) {
            commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
        } finally {
            out.close();
        }
        return fileName;
    }

    @When("^I delete '(certificate|keytab|password)' '(.+?)'( located in path '(.+?)')?$")
    public void removeSecret(String secretType, String secret, String path) throws Exception {
        String baseUrl = "/service/deploy-api/secrets";
        String secretTypeAux;
        switch (secretType) {
            case "certificate":
                secretTypeAux = "certificates";
                break;
            case "keytab":
                secretTypeAux = "kerberos";
                break;
            case "password":
                secretTypeAux = "passwords";
                break;
            default:
                secretTypeAux = "default";
        }
        String pathAux = path != null ? path.replaceAll("/", "%2F") + secret : "%2Fuserland%2F" + secretTypeAux + "%2F" + secret;
        sendRequestNoDataTable("DELETE", baseUrl + "?path=" + pathAux, null, null, null);
        sendRequestNoDataTable("GET", baseUrl + "?path=" + pathAux, null, null, null);
        assertResponseStatusLength(404, null, null);
    }

    private String getCertificateUrlParams(String secret, String path, String cn, String name, String alt, String organizationName) {
        String pathAux = path != null ? path.replaceAll("/", "%2F") + secret : "%2Fuserland%2Fcertificates%2F" + secret;
        String cnAux = cn != null ? cn : secret;
        String nameAux = name != null ? name : secret;
        String urlParams = "?path=" + pathAux + "&cn=" + cnAux + "&name=" + nameAux;
        if (alt != null) {
            urlParams = urlParams + "&alt=" + alt;
        }
        if (organizationName != null) {
            urlParams = urlParams + "&organizationName=" + organizationName;
        }
        return urlParams;
    }

    private String getKeytabUrlParams(String secret, String path, String name, String principal, String realm) throws Exception {
        String pathAux = path != null ? path.replaceAll("/", "%2F") + secret : "%2Fuserland%2Fkerberos%2F" + secret;
        String principalAux = principal != null ? principal : secret;
        String nameAux = name != null ? name : secret;
        String realmAux = realm != null ? realm : ThreadProperty.get("EOS_REALM");
        if (realmAux == null) {
            throw new Exception("Realm is mandatory to generate keytab");
        }
        return "?path=" + pathAux + "&principal=" + principalAux + "&name=" + nameAux + "&realm=" + realmAux;
    }

    private String getPasswordUrlParams(String secret, String path, String name, String user, String password) {
        String pathAux = path != null ? path.replaceAll("/", "%2F") + secret : "%2Fuserland%2Fpasswords%2F" + secret;
        String nameAux = name != null ? name : secret;
        String userAux = user != null ? user : secret;
        String passwordAux = password != null ? password : secret;
        return "?path=" + pathAux + "&name=" + nameAux + "&password=" + passwordAux + "&user=" + userAux;
    }

    @When("^I include group '(.+?)' in profile '(.+?)'$")
    public void includeGroupInProfile(String groupId, String profileId) throws Exception {
        String endPointGetGroup = "/service/gosecmanagement/api/group?id=" + groupId;
        String endPointGetProfile = "/service/gosecmanagement/api/profile?id=" + profileId;
        String groups = "groups";
        String pid = "pid";
        String id = "id";
        String roles = "roles";
        Boolean content = false;

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

        sendRequestNoDataTable("GET", endPointGetGroup, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            JsonObject jsonGroupInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
            sendRequestNoDataTable("GET", endPointGetProfile, null, null, null);
            if (commonspec.getResponse().getStatusCode() == 200) {
                JsonObject jsonProfileInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                //Get groups from profile
                JsonArray jsonGroups = (JsonArray) jsonProfileInfo.get(groups);
                //Get size of groups
                String[] stringGroups = new String[jsonGroups.size() + 1];
                //Create json for put
                JSONObject putObject = new JSONObject(commonspec.getResponse().getResponse());
                //Remove groups and roles in json
                putObject.remove(groups);
                putObject.remove(roles);

                for (int i = 0; i < jsonGroups.size(); i++) {
                    String jsonIds = ((JsonObject) jsonGroups.get(i)).getString("id", "");

                    if (jsonIds.equals(groupId)) {
                        commonspec.getLogger().warn("{} is already included in the profile {}", groupId, profileId);
                        content = true;
                        break;
                    } else {
                        stringGroups[i] = jsonIds;
                    }
                }

                if (!content) {
                    //Add new group in array of gids
                    stringGroups[jsonGroups.size()] = groupId;
                    //Add gids array to new json for PUT request
                    putObject.put("gids", stringGroups);

                    commonspec.getLogger().warn("Json for PUT request---> {}", putObject.toString());
                    Future<Response> response = commonspec.generateRequest("PUT", false, null, null, endPointGetProfile, JsonValue.readHjson(putObject.toString()).toString(), "json", "");
                    commonspec.setResponse("PUT", response.get());
                    if (commonspec.getResponse().getStatusCode() != 204) {
                        throw new Exception("Error adding Group: " + groupId + " in Profile " + profileId + " - Status code: " + commonspec.getResponse().getStatusCode());
                    }
                }

            } else {
                throw new Exception("Error obtaining Profile: " + profileId + "- Status code: " + commonspec.getResponse().getStatusCode());
            }

        } else {
            throw new Exception("Error obtaining Group: " + groupId + "- Status code: " + commonspec.getResponse().getStatusCode());
        }
    }

}
