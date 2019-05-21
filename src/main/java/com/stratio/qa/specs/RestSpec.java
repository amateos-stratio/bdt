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
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.WritableAssertionInfo;
import org.json.JSONArray;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;

/**
 * Generic API Rest Specs.
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
    @Given("^in less than '(\\d+?)' seconds, checking each '(\\d+?)' seconds, I send a '(.+?)' request to '(.+?)' so that the response( does not)? contains '(.+?)' based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void sendRequestDataTableTimeout(Integer timeout, Integer wait, String requestType, String endPoint, String contains, String responseVal, String baseData, String baz, String type, DataTable modifications) throws Exception {

        // Retrieve data
        String retrievedData = commonspec.retrieveData(baseData, type);

        // Modify data
        commonspec.getLogger().debug("Modifying data {} as {}", retrievedData, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();

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
            commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", requestType, endPoint, modifiedData, type);
            response = commonspec.generateRequest(requestType, false, null, null, endPoint, modifiedData, type);
            commonspec.getLogger().debug("Saving response");
            commonspec.setResponse(requestType, response.get());
            commonspec.getLogger().debug("Checking response value");
            try {
                if (searchUntilContains) {
                    assertThat(commonspec.getResponse().getResponse()).containsPattern(pattern);
                    found = true;
                    timeout = i;
                } else {
                    assertThat(commonspec.getResponse().getResponse()).doesNotContain(responseVal);
                    found = false;
                    timeout = i;
                }
            } catch (AssertionError e) {
                if (!found) {
                    commonspec.getLogger().info("Response value not found after " + i + " seconds");
                } else {
                    commonspec.getLogger().info("Response value found after " + i + " seconds");
                }
                Thread.sleep(wait * 1000);
                ex = e;
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
     * @param foo           parameter generated by cucumber because of the optional expression
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
    public void sendRequest(String requestType, String endPoint, String foo, String loginInfo, String baseData, String baz, String type, DataTable modifications) throws Exception {
        // Retrieve data
        String retrievedData = commonspec.retrieveData(baseData, type);

        // Modify data
        commonspec.getLogger().debug("Modifying data {} as {}", retrievedData, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();

        String user = null;
        String password = null;
        if (loginInfo != null) {
            user = loginInfo.substring(0, loginInfo.indexOf(':'));
            password = loginInfo.substring(loginInfo.indexOf(':') + 1, loginInfo.length());
        }


        commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", requestType, endPoint, modifiedData, type);
        Future<Response> response = commonspec.generateRequest(requestType, false, user, password, endPoint, modifiedData, type, "");

        // Save response
        commonspec.getLogger().debug("Saving response");
        commonspec.setResponse(requestType, response.get());
    }


    /**
     *Creates a custom user in gosec management if the userId doesn't exist
     *
     * @param userId
     * @param endPoint
     * @param foo
     * @param loginInfo
     * @param baseData
     * @param baz
     * @param type
     * @param modifications
     * @throws Exception
     */
    @When("^I create user '(.+?)' to '(.+?)'( with user and password '(.+:.+?)')? if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createUserIfNotExist(String userId, String endPoint, String foo, String loginInfo, String baseData, String baz, String type, DataTable modifications) throws Exception {
        // Retrieve data
        String retrievedData = commonspec.retrieveData(baseData, type);

        // Modify data
        commonspec.getLogger().debug("Modifying data {} as {}", retrievedData, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();

        Integer expectedStatusCreate = new Integer(201);
        String endPointUser = endPoint + "/" + userId;
        String user = null;
        String password = null;

        if (loginInfo != null) {
            user = loginInfo.substring(0, loginInfo.indexOf(':'));
            password = loginInfo.substring(loginInfo.indexOf(':') + 1, loginInfo.length());
        }

        //Get user
        commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", new Object[]{"GET", endPointUser, null, null});
        Future<Response> response = commonspec.generateRequest("GET", false, user, password, endPointUser, null, null, "");
        commonspec.getLogger().debug("Saving response");
        commonspec.setResponse("GET", (Response) response.get());

        if (commonspec.getResponse().getStatusCode() != 200) {
            //Create user if doesn't exist
            commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", new Object[]{"POST", endPoint, modifiedData, type});
            Future<Response> responseCreateUser = commonspec.generateRequest("POST", false, user, password, endPoint, modifiedData, type, "");
            commonspec.getLogger().debug("Saving response");
            commonspec.setResponse("POST", (Response) responseCreateUser.get());

            try {
                if (commonspec.getResponse().getStatusCode() == 409) {
                    commonspec.getLogger().warn("User {} already exists", userId);
                } else {
                    assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                    commonspec.getLogger().warn("User {} created", userId);
                }
            } catch (Exception e) {
                commonspec.getLogger().warn("Error creating user {}: {}", userId, commonspec.getResponse().getResponse());
                throw e;
            }

        }
    }


    /**
     *Creates a custom group in gosec management if the userId doesn't exist
     *
     * @param groupId
     * @param endPoint
     * @param foo
     * @param loginInfo
     * @param baseData
     * @param baz
     * @param type
     * @param modifications
     * @throws Exception
     */
    @When("^I create group '(.+?)' to '(.+?)'( with user and password '(.+:.+?)')? if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createGroupIfNotExist(String groupId, String endPoint, String foo, String loginInfo, String baseData, String baz, String type, DataTable modifications) throws Exception {
        // Retrieve data
        String retrievedData = commonspec.retrieveData(baseData, type);

        // Modify data
        commonspec.getLogger().debug("Modifying data {} as {}", retrievedData, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();

        Integer expectedStatusCreate = new Integer(201);
        String endPointGroup = endPoint + "/" + groupId;
        String user = null;
        String password = null;

        if (loginInfo != null) {
            user = loginInfo.substring(0, loginInfo.indexOf(':'));
            password = loginInfo.substring(loginInfo.indexOf(':') + 1, loginInfo.length());
        }

        //Get Group
        commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", new Object[]{"GET", endPointGroup, null, null});
        Future<Response> response = commonspec.generateRequest("GET", false, user, password, endPointGroup, null, null, "");
        commonspec.getLogger().debug("Saving response");
        commonspec.setResponse("GET", (Response) response.get());

        if (commonspec.getResponse().getStatusCode() != 200) {
            //Create group if doesn't exist
            commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", new Object[]{"POST", endPoint, modifiedData, type});
            Future<Response> responseCreateGroup = commonspec.generateRequest("POST", false, user, password, endPoint, modifiedData, type, "");
            commonspec.getLogger().debug("Saving response");
            commonspec.setResponse("POST", (Response) responseCreateGroup.get());

            try {
                if (commonspec.getResponse().getStatusCode() == 409) {
                    commonspec.getLogger().warn("Group {} already exists", groupId);
                } else {
                    assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                    commonspec.getLogger().warn("Group {} created", groupId);
                }
            } catch (Exception e) {
                commonspec.getLogger().warn("Error creating Group {}: {}", groupId, commonspec.getResponse().getResponse());
                throw e;
            }

        }
    }

    /**
     *Creates a policy in gosec management if the policyId doesn't exist. If it exists, the old one is deleted before the creation.
     *
     * @param policyId
     * @param endPoint
     * @param foo
     * @param loginInfo
     * @param baseData
     * @param baz
     * @param type
     * @param modifications
     * @throws Exception
     */
    @When("^I create policy '(.+?)' to '(.+?)'( with user and password '(.+:.+?)')? if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createPolicyIfNotExist(String policyId, String endPoint, String foo, String loginInfo, String baseData, String baz, String type, DataTable modifications) throws Exception {
        // Retrieve data
        String retrievedData = commonspec.retrieveData(baseData, type);

        // Modify data
        commonspec.getLogger().debug("Modifying data {} as {}", retrievedData, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();

        Integer expectedStatusCreate = new Integer(201);
        Integer expectedStatusDelete = new Integer(200);
        String endPointPolicy = endPoint + "/" + policyId;
        String user = null;
        String password = null;

        if (loginInfo != null) {
            user = loginInfo.substring(0, loginInfo.indexOf(':'));
            password = loginInfo.substring(loginInfo.indexOf(':') + 1, loginInfo.length());
        }

        //Get Policy
        commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", new Object[]{"GET", endPointPolicy, null, null});
        Future<Response> response = commonspec.generateRequest("GET", false, user, password, endPointPolicy, null, null, "");
        commonspec.getLogger().debug("Saving response");
        commonspec.setResponse("GET", (Response) response.get());

        if (commonspec.getResponse().getStatusCode() == 200) {
            //Delete policy if exists
            commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", new Object[]{"DELETE", endPointPolicy, null, null});
            Future<Response> responseDeletePolicy = commonspec.generateRequest("DELETE", false, user, password, endPointPolicy, null, null, "");
            commonspec.getLogger().debug("Saving response");
            commonspec.setResponse("DELETE", (Response) responseDeletePolicy.get());

            try {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusDelete);
            } catch (Exception e) {
                commonspec.getLogger().warn("Error deleting Policy {}: {}", policyId, commonspec.getResponse().getResponse());
                throw e;
            }
        }

        //Create policy
        commonspec.getLogger().debug("Generating request {} to {} with data {} as {}", new Object[]{"POST", endPoint, modifiedData, type});
        Future<Response> responseCreatePolicy = commonspec.generateRequest("POST", false, user, password, endPoint, modifiedData, type, "");
        commonspec.getLogger().debug("Saving response");
        commonspec.setResponse("POST", (Response) responseCreatePolicy.get());

        try {
            if (commonspec.getResponse().getStatusCode() == 409) {
                commonspec.getLogger().warn("Policy {} already exists", policyId);
            } else {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                commonspec.getLogger().warn("Policy {} created", policyId);
            }
        } catch (Exception e) {
            commonspec.getLogger().warn("Error creating Policy {}: {}", policyId, commonspec.getResponse().getResponse());
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
     * @param foo
     * @param baseData
     * @param bar
     * @param type
     * @throws Exception
     */
    @When("^I send a '(.+?)' request to '(.+?)'( with user and password '(.+:.+?)')?( based on '([^:]+?)')?( as '(json|string|gov)')?$")
    public void sendRequestNoDataTable(String requestType, String endPoint, String foo, String loginInfo, String
            bar, String baseData, String baz, String type) throws Exception {
        Future<Response> response;
        String user = null;
        String password = null;

        if (loginInfo != null) {
            user = loginInfo.substring(0, loginInfo.indexOf(':'));
            password = loginInfo.substring(loginInfo.indexOf(':') + 1, loginInfo.length());
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
    @When("^in less than '(\\d+?)' seconds, checking each '(\\d+?)' seconds, I send a '(.+?)' request to '(.+?)'( so that the response( does not)? contains '(.+?)')?$")
    public void sendRequestTimeout(Integer timeout, Integer wait, String requestType, String endPoint, String
            foo, String contains, String responseVal) throws Exception {

        AssertionError ex = null;
        String type = "";
        Future<Response> response;

        if (foo != null) {
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
                response = commonspec.generateRequest(requestType, false, null, null, endPoint, "", type, "");
                commonspec.setResponse(requestType, response.get());
                commonspec.getLogger().debug("Checking response value");
                try {
                    if (searchUntilContains) {
                        assertThat(commonspec.getResponse().getResponse()).containsPattern(pattern);
                        found = true;
                        timeout = i;
                    } else {
                        assertThat(commonspec.getResponse().getResponse()).doesNotContain(responseVal);
                        found = false;
                        timeout = i;
                    }
                } catch (AssertionError e) {
                    if (!found) {
                        commonspec.getLogger().info("Response value not found after " + i + " seconds");
                    } else {
                        commonspec.getLogger().info("Response value found after " + i + " seconds");
                    }
                    Thread.sleep(wait * 1000);
                    ex = e;
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
                response = commonspec.generateRequest(requestType, false, null, null, endPoint, "", type, "");
                commonspec.setResponse(requestType, response.get());
                commonspec.getLogger().debug("Checking response value");
                try {
                    assertThat(commonspec.getResponse().getResponse());
                    timeout = i;
                } catch (AssertionError e) {
                    Thread.sleep(wait * 1000);
                    ex = e;
                }
            }
        }
    }

    @When("^I login to '(.+?)' based on '([^:]+?)' as '(json|string)'$")
    public void loginUser(String endPoint, String baseData, String type) throws Exception {
        sendRequestNoDataTable("POST", endPoint, null, null, null, baseData, null, type);
    }

    @When("^I login to '(.+?)' based on '([^:]+?)' as '(json|string)' with:$")
    public void loginUser(String endPoint, String baseData, String type, DataTable modifications) throws Exception {
        sendRequest("POST", endPoint, null, null, baseData, "", type, modifications);
    }

    @When("^I logout from '(.+?)'$")
    public void logoutUser(String endPoint) throws Exception {
        sendRequestNoDataTable("GET", endPoint, null, null, null, "", null, "");
    }

    @Then("^the service response must contain the text '(.*?)'$")
    public void assertResponseMessage(String expectedText) throws
            ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Pattern pattern = CommonG.matchesOrContains(expectedText);
        assertThat(commonspec.getResponse().getResponse()).containsPattern(pattern);
    }

    @Then("^the service response must not contain the text '(.*?)'$")
    public void assertNegativeResponseMessage(String expectedText) throws
            ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        assertThat(commonspec.getResponse().getResponse()).doesNotContain(expectedText);
    }

    @Then("^the service response status must be '(.*?)'( and its response length must be '(.*?)' | and its response must contain the text '(.*?)')?$")
    public void assertResponseStatusLength(Integer expectedStatus, String foo, Integer expectedLength, String
            expectedText) {
        if (foo != null) {
            if (foo.contains("length")) {
                assertThat(Optional.of(commonspec.getResponse())).hasValueSatisfying(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(expectedStatus);
                    assertThat((new JSONArray(r.getResponse())).length()).isEqualTo(expectedLength);
                });
            } else if (foo.contains("text")) {
                WritableAssertionInfo assertionInfo = new WritableAssertionInfo();
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

    @Then("^I save service response in environment variable '(.*?)'( and file '(.*?)')?$")
    public void saveResponseInEnvironmentVariableFile(String envVar, String foo, String fileName) throws Exception {
        String value = commonspec.getResponse().getResponse();

        ThreadProperty.set(envVar, value);

        if (foo != null) {
            // Create file (temporary) and set path to be accessible within test
            File tempDirectory = new File(String.valueOf(System.getProperty("user.dir") + "/target/test-classes/"));
            String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
            commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
            // Note that this Writer will delete the file if it exists
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), "UTF-8"));
            try {
                out.write(value);
            } catch (Exception e) {
                commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
            } finally {
                out.close();
            }

            Assertions.assertThat(new File(absolutePathFile).isFile());
        }
    }

}
