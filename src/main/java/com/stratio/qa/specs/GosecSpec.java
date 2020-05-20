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
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Generic Gosec Specs.
 *
 * @see <a href="GosecSpec-annotations.html">Gosec Steps &amp; Matching Regex</a>
 */
public class GosecSpec extends BaseGSpec {

    private final Logger logger = LoggerFactory.getLogger(GosecSpec.class);

    RestSpec restSpec;

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public GosecSpec(CommonG spec) {
        this.commonspec = spec;
        restSpec = new RestSpec(spec);
    }

    /**
     * Create resource in Gosec
     *
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param endPoint        : endpoint to send request to (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @param baseData        : base information to use for request
     * @param type            : type of data (enum value) (OPTIONAL)
     * @param modifications   : modifications to perform oven base data
     * @throws Exception
     */
    @When("^I create '(policy|user|group)' '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( using API service path '(.+?)')?( with user and password '(.+:.+?)')? based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createResource(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        createResourceIfNotExist(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, false, baseData, type, modifications);

    }

    /**
     * Create resource in Gosec if it doesn exist already
     *
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param endPoint        : endpoint to send request to (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @param baseData        : base information to use for request
     * @param type            : type of data (enum value) (OPTIONAL)
     * @param modifications   : modifications to perform oven base data
     * @throws Exception
     */
    @When("^I create '(policy|user|group)' '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( using API service path '(.+?)')?( with user and password '(.+:.+?)')? if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createResourceIfNotExist(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        createResourceIfNotExist(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, true, baseData, type, modifications);
    }

    /**
     * Creates a custom resource in gosec management if the resource doesn't exist
     *
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant where resource is gonna be created
     * @param tenantLoginInfo : user and password to log into tenant
     * @param endPoint        : endpoint to send request to
     * @param loginInfo       : user and password to log in service
     * @param doesNotExist    : (if 'empty', creation is forced deleting the previous policy if exists)
     * @param baseData        : base information to use for request
     * @param type            : type of data (enum value)
     * @param modifications   : modifications to perform oven base data
     * @throws Exception
     */
    private void createResourceIfNotExist(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, boolean doesNotExist, String baseData, String type, DataTable modifications) throws Exception {
        Integer expectedStatusCreate = 201;
        Integer expectedStatusDelete = 200;
        String endPointResource = "";
        String endPointPolicies = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String endPointPolicy = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
        String newEndPoint = "";
        String gosecVersion = ThreadProperty.get("gosec-management_version");
        List<List<String>> newModifications;
        newModifications = convertDataTableToModifiableList(modifications);
        Boolean addSourceType = false;

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (endPoint != null) {
            endPointResource = endPoint + resourceId;

            if (endPoint.contains("id")) {
                newEndPoint = endPoint.replace("?id=", "");
            } else {
                newEndPoint = endPoint.substring(0, endPoint.length() - 1);
            }
        } else {
            if (resource.equals("policy")) {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
            } else {
                if (resource.equals("user")) {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
                } else {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
                }
            }
            if (endPoint.contains("id")) {
                newEndPoint = endPoint.replace("?id=", "");
            } else {
                newEndPoint = endPoint.substring(0, endPoint.length() - 1);
            }
            endPointResource = endPoint + resourceId;
        }

        if (gosecVersion != null) {
            String[] gosecVersionArray = gosecVersion.split("\\.");
            // Add inputSourceType if Gosec >= 1.4.x
            if (Integer.parseInt(gosecVersionArray[0]) >= 1 && Integer.parseInt(gosecVersionArray[1]) >= 4) {
                addSourceType = true;
            }
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                    String policyId = commonspec.getCommandResult().trim();
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPointPolicy + policyId;
                    } else {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                        policyId = commonspec.getCommandResult().trim();
                        if (!policyId.equals("")) {
                            commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                            endPointResource = endPointPolicy + policyId;
                        } else {
                            endPointResource = endPointPolicy + "thisIsANewPolicyId";
                        }
                    }
                }
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {
                if (resource.equals("user") && (addSourceType)) {
                    commonspec.getLogger().warn("Gosec Version:{} -> Adding inputsourceType = CUSTOM", gosecVersion);
                    List<String> newField = Arrays.asList("$.inputSourceType", "ADD", "CUSTOM", "string");
                    newModifications.add(newField);
                }
                // Create datatable with modified data
                DataTable gosecModifications = DataTable.create(newModifications);
                // Send POST request
                restSpec.sendRequest("POST", newEndPoint, loginInfo, baseData, type, gosecModifications);
                try {
                    if (commonspec.getResponse().getStatusCode() == 409) {
                        commonspec.getLogger().warn("The resource {} already exists", resourceId);
                    } else {
                        try {
                            assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                        } catch (AssertionError e) {
                            commonspec.getLogger().warn("Error creating Resource {}: {}", resourceId, commonspec.getResponse().getResponse());
                            throw e;
                        }
                        commonspec.getLogger().warn("Resource {} created", resourceId);
                    }
                } catch (Exception e) {
                    commonspec.getLogger().warn("Error creating user {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().warn("{}:{} already exist", resource, resourceId);
                if (resource.equals("policy") && commonspec.getResponse().getStatusCode() == 200) {
                    if (doesNotExist) {
                        // Policy already exists
                        commonspec.getLogger().warn("Policy {} already exist - not created", resourceId);

                    } else {
                        // Delete policy if exists
                        restSpec.sendRequest("DELETE", endPointResource, loginInfo, baseData, type, modifications);
                        commonspec.getLogger().warn("Policy {} deleted", resourceId);

                        try {
                            assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusDelete);
                        } catch (AssertionError e) {
                            commonspec.getLogger().warn("Error deleting Policy {}: {}", resourceId, commonspec.getResponse().getResponse());
                            throw e;
                        }
                        createResourceIfNotExist(resource, resourceId, endPoint, tenantOrig, tenantLoginInfo, loginInfo, doesNotExist, baseData, type, modifications);
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
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant resource is gonna be deleted from (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param endPoint        : endpoint to send request to (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @throws Exception
     */
    @When("^I delete '(policy|user|group)' '(.+?)'( from tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( using API service path '(.+?)')?( with user and password '(.+:.+?)')? if it exists$")
    public void deleteUserIfExists(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo) throws Exception {
        Integer[] expectedStatusDelete = {200, 204};
        String endPointResource = "";
        String endPointPolicy = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
        String endPointPolicies = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (!resource.equals("policy")) {
            resourceId = resourceId.replaceAll("\\s+", ""); //delete white spaces
        }

        if (managementBaasVersion != null) {
            endPointPolicies = "/service/gosec-management-baas/management/policies";
            endPointPolicy = "/service/gosec-management-baas/management/policies/";
        }

        if (endPoint != null) {
            endPointResource = endPoint + resourceId;
        } else {
            if (resource.equals("policy")) {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
                if (managementBaasVersion != null) {
                    endPoint = "/service/gosec-management-baas/management/policies/";
                }
            } else {
                if (resource.equals("user")) {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
                    if (managementBaasVersion != null) {
                        endPoint = "/service/gosec-management-baas/management/users/";
                    }
                } else {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
                    if (managementBaasVersion != null) {
                        endPoint = "/service/gosec-management-baas/management/groups/";
                    }
                }
            }
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    if (managementBaasVersion != null) {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").pid' | sed s/\\\"//g");
                        String policyId = commonspec.getCommandResult().trim();
                        if (!policyId.equals("")) {
                            commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                            endPointResource = endPointPolicy + policyId;
                        }
                    } else {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                        String policyId = commonspec.getCommandResult().trim();
                        if (!policyId.equals("")) {
                            commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                            endPointResource = endPointPolicy + policyId;
                        } else {
                            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                            policyId = commonspec.getCommandResult().trim();
                            if (!policyId.equals("")) {
                                commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                                endPointResource = endPointPolicy + policyId;
                            } else {
                                endPointResource = endPointPolicy + "thisPolicyDoesNotExistId";
                            }
                        }
                    }
                }
            } else {
                endPointResource = endPoint + resourceId;
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                // Delete resource if exists
                restSpec.sendRequestNoDataTable("DELETE", endPointResource, loginInfo, null, null);
                commonspec.getLogger().warn("Resource {} deleted", resourceId);

                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                } catch (AssertionError e) {
                    commonspec.getLogger().warn("Error deleting Resource {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().warn("Resource {} with id {} not found so it's not deleted", resource, resourceId);
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}: {}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    /**
     * Retrieve id from policy
     *
     * @param tag             : whether it is a tag policy or not (OPTIONAL)
     * @param policyName      : policy name to obtain id from
     * @param tenantOrig      : tenant where policy lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param envVar          : thread variable where to store result
     * @throws Exception
     */
    @When("^I get id from( tag)? policy with name '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( with user and password '(.+:.+?)')? and save it in environment variable '(.+?)'$")
    public void getPolicyId(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String loginInfo, String envVar) throws Exception {
        String endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");

        if (managementBaasVersion != null) {
            endPoint = "/service/gosec-management-baas/management/policies";
        }

        if (tag != null) {
            endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_TAGS");
            if (managementBaasVersion != null) {
                endPoint = "/service/gosec-management-baas/management/policies/tags";
            }
        }

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            if (managementBaasVersion != null) {
                commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").pid' | sed s/\\\"//g");
                commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
                if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                    fail("Error obtaining ID from policy " + policyName);
                }
            } else {
                commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
                if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                    commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
                    if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                        fail("Error obtaining ID from policy " + policyName);
                    }
                }
            }
        } else {
            if (commonspec.getResponse().getStatusCode() == 404) {
                fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", endPoint);
            }
        }
    }

    /**
     * Create tenant in cluster
     *
     * @param tenantId      : name of the tenant to be created
     * @param baseData      : base information to use for request
     * @param type          : type of base info (enum value) (OPTIONAL)
     * @param modifications : modifications to perform over base data
     * @throws Exception
     */
    @When("^I create tenant '(.+?)' if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createTenant(String tenantId, String baseData, String type, DataTable modifications) throws Exception {
        // Set REST connection
        commonspec.setCCTConnection(null, null);

        String endPoint = "/service/gosec-identities-daas/identities/tenants";
        String endPointResource = endPoint + "/" + tenantId;
        Integer expectedStatus = 201;
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.getLogger().warn("Tenant {} already exist - not created", tenantId);
        } else {
            restSpec.sendRequest("POST", endPoint, null, baseData, type, modifications);
            try {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatus);
            } catch (AssertionError e) {
                commonspec.getLogger().warn("Error creating Tenant {}: {}", tenantId, commonspec.getResponse().getResponse());
                throw e;
            }
        }
    }

    /**
     * Delete tenant
     *
     * @param tenantId : tenant to be deleted
     * @throws Exception
     */

    @When("^I delete tenant '(.+?)' if it exists$")
    public void deleteTenant(String tenantId) throws Exception {
        // Set REST connection
        commonspec.setCCTConnection(null, null);

        String endPoint = "/service/gosec-identities-daas/identities/tenants";
        String endPointResource = endPoint + "/" + tenantId;
        Integer expectedStatus = 204;
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            restSpec.sendRequestNoDataTable("DELETE", endPointResource, null, null, null);
            commonspec.getLogger().warn("Tenant {} deleted", tenantId);
            try {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatus);
            } catch (AssertionError e) {
                commonspec.getLogger().warn("Error deleting Tenant {}: {}", tenantId, commonspec.getResponse().getResponse());
                throw e;
            }
        } else {
            commonspec.getLogger().warn("Tenant {} does not exist - not deleted", tenantId);
        }
    }

    /**
     * Include resource in tenant
     *
     * @param resource   : resource type to be included (enum value)
     * @param resourceId : resource name
     * @param tenantId   : tenant where to store resource in
     * @throws Exception
     */
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

        // Set REST connection
        commonspec.setCCTConnection(null, null);

        restSpec.sendRequestNoDataTable("GET", endPointGosec, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            if (commonspec.getResponse().getResponse().contains("\"" + uidOrGid + "\":\"" + resourceId + "\"")) {
                restSpec.sendRequestNoDataTable("GET", endPointTenant, null, null, null);
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

    /**
     * Obtain id from profile
     *
     * @param profileName     : profile to obtain id from
     * @param tenantOrig      : tenant where policy lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param envVar          : thread variable where to store result
     * @throws Exception
     */
    @When("^I get id from profile with name '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')? and save it in environment variable '(.+?)'$")
    public void getProfiled(String profileName, String tenantOrig, String tenantLoginInfo, String envVar) throws Exception {
        String endPoint = "/service/gosec-identities-daas/identities/profiles";
        if (tenantOrig != null) {
            endPoint = "/service/gosec-identities-daas/identities/profiles?tid=" + tenantOrig;
        }

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPoint, null, null, null);
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

    /**
     * Obtain json from policy
     *
     * @param tag             : whether it is a tag policy or not (OPTIONAL)
     * @param policyName      : policy name to obtain json from
     * @param tenantOrig      : tenant where policy lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param envVar          : thread variable where to store result (OPTIONAL)
     * @param fileName        : file name where to store result (OPTIONAL)
     * @throws Exception
     */
    @When("^I get json from( tag)? policy with name '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( with user and password '(.+:.+?)')? and save it( in environment variable '(.*?)')?( in file '(.*?)')?$")
    public void getPolicyJson(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String loginInfo, String envVar, String fileName) throws Exception {
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

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
            if (commonspec.getCommandResult().trim().equals("")) {
                commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
            }
            restSpec.sendRequestNoDataTable("GET", "/service/gosecmanagement/api/policy/" + commonspec.getCommandResult(), null, null, null);

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
                restSpec.sendRequestNoDataTable("GET", newEndPoint, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                    if (commonspec.getCommandResult().trim().equals("")) {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                    }
                    restSpec.sendRequestNoDataTable("GET", "/service/gosecmanagement/api/policy?id=" + commonspec.getCommandResult(), null, null, null);

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

    /**
     * Include group inside profile
     *
     * @param groupId         : id of the group to be included in profile
     * @param profileId       : id of the profile where to include group
     * @param tenantOrig      : tenant where profile lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @throws Exception
     */
    @When("^I include group '(.+?)' in profile '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( with user and password '(.+:.+?)')?$")
    public void includeGroupInProfile(String groupId, String profileId, String tenantOrig, String tenantLoginInfo, String loginInfo) throws Exception {
        String endPointGetGroup = "/service/gosecmanagement/api/group?id=" + groupId;
        String endPointGetProfile = "/service/gosecmanagement/api/profile?id=" + profileId;
        String groups = "groups";
        String pid = "pid";
        String id = "id";
        String roles = "roles";
        Boolean content = false;

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

        restSpec.sendRequestNoDataTable("GET", endPointGetGroup, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            JsonObject jsonGroupInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
            restSpec.sendRequestNoDataTable("GET", endPointGetProfile, null, null, null);
            if (commonspec.getResponse().getStatusCode() == 200) {
                JsonObject jsonProfileInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                // Get groups from profile
                JsonArray jsonGroups = (JsonArray) jsonProfileInfo.get(groups);
                // Get size of groups
                String[] stringGroups = new String[jsonGroups.size() + 1];
                // Create json for put
                JSONObject putObject = new JSONObject(commonspec.getResponse().getResponse());
                // Remove groups and roles in json
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
                    // Add new group in array of gids
                    stringGroups[jsonGroups.size()] = groupId;
                    // Add gids array to new json for PUT request
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

    /**
     * Convert DataTable to modifiable list
     *
     * @param dataTable : DataTable data
     * @return
     */
    private List<List<String>> convertDataTableToModifiableList(DataTable dataTable) {
        List<List<String>> lists = dataTable.asLists(String.class);
        List<List<String>> updateableLists = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            List<String> list = lists.get(i);
            List<String> updateableList = new ArrayList<>();
            for (int j = 0; j < list.size(); j++) {
                updateableList.add(j, list.get(j));
            }
            updateableLists.add(i, updateableList);
        }
        return updateableLists;
    }

    /**
     * Updates a resource in gosec management if the resourceId exists previously.
     *
     * @param resource        : type of resource (policy, user, group or tenant)
     * @param resourceId      : policy name, userId, groupId or tenantId
     * @param tenantOrig      : tenant where resource lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @param type            : type of data (json,string,gov) (OPTIONAL)
     * @param modifications   : data to modify the resource
     * @throws Exception if the resource does not exists or the request fails
     */
    @When("^I update '(policy|user|group|tenant)' '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( with user and password '(.+:.+?)')? based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void updateResource(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        Integer[] expectedStatusUpdate = {200, 201, 204};
        String endPointPolicy = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
        String endPointPolicies = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String endPoint = "";
        String endPointResource = "";

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (resource.equals("policy")) {
            endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
        } else {
            if (resource.equals("user")) {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
            } else {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
            }
            if (resource.equals("tenant")) {
                endPoint = "/service/gosec-identities-daas/identities/tenants/";
            }
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                    String policyId = commonspec.getCommandResult().trim();
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPointPolicy + policyId;
                    } else {
                        commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.[] | select (.name == \"" + resourceId + "\").id' | sed s/\\\"//g");
                        policyId = commonspec.getCommandResult().trim();
                        if (!policyId.equals("")) {
                            commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                            endPointResource = endPointPolicy + policyId;
                        } else {
                            endPointResource = endPointPolicy + "thisPolicyDoesNotExistId";
                        }
                    }
                }
            } else {
                endPointResource = endPoint + resourceId;
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                if (resource.equals("tenant")) {
                    restSpec.sendRequest("PATCH", endPointResource, loginInfo, baseData, type, modifications);
                } else {
                    restSpec.sendRequest("PUT", endPointResource, loginInfo, baseData, type, modifications);
                }
                commonspec.getLogger().warn("Resource {}:{} updated", resource, resourceId);

                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusUpdate);
                } catch (AssertionError e) {
                    commonspec.getLogger().error("Error updating Resource {} {}: {}", resource, resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().error("Resource {}:{} not found so it's not updated", resource, resourceId);
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}: {}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    /**
     * Removes user or group from tenant if the resource exists and has been assigned previously
     *
     * @param resource   : type of resource (user or group)
     * @param resourceId : userId or groupId
     * @param tenantId   : tenant to remove resource from
     * @throws Exception if the resource does not exists or the request fails
     */
    @When("^I remove '(user|group)' '(.+?)' from tenant '(.+?)'$")
    public void removeResourceInTenant(String resource, String resourceId, String tenantId) throws Exception {
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

        // Set REST connection
        commonspec.setCCTConnection(null, null);

        restSpec.sendRequestNoDataTable("GET", endPointGosec, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            if (commonspec.getResponse().getResponse().contains("\"" + uidOrGid + "\":\"" + resourceId + "\"")) {
                restSpec.sendRequestNoDataTable("GET", endPointTenant, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    JsonObject jsonTenantInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                    if (((JsonArray) jsonTenantInfo.get(uidOrGidTenant)).values().contains(JsonValue.valueOf(resourceId))) {
                        // remove resource from tenant
                        // Get groups/users from tenant
                        JsonArray jsonGroups = (JsonArray) jsonTenantInfo.get(uidOrGidTenant);
                        // Create new string for new data without resource
                        String[] stringGroups = new String[jsonGroups.size() - 1];
                        // Create json for put
                        JSONObject putObject = new JSONObject(commonspec.getResponse().getResponse());
                        // Remove ids in json
                        putObject.remove(uidOrGidTenant);
                        // create new array with values without resourceId
                        for (int i = 0; i < jsonGroups.size(); i++) {
                            int j = 0;
                            String jsonIds = jsonGroups.get(i).toString().substring(1, jsonGroups.get(i).toString().length() - 1);
                            if (jsonIds.equals(resourceId)) {
                                commonspec.getLogger().warn("{} {} removed from tenant {}", resource, resourceId, tenantId);
                            } else {
                                stringGroups[j] = jsonIds;
                                j = j + 1;
                            }
                        }
                        putObject.put(uidOrGidTenant, stringGroups);
                        commonspec.getLogger().debug("Json for PATCH request---> {}", putObject.toString());
                        Future<Response> response = commonspec.generateRequest("PATCH", false, null, null, endPointTenant, JsonValue.readHjson(putObject.toString()).toString(), "json", "");
                        commonspec.setResponse("PATCH", response.get());
                        if (commonspec.getResponse().getStatusCode() != 204) {
                            throw new Exception("Error removing " + resource + " " + resourceId + " in tenant " + tenantId + " - Status code: " + commonspec.getResponse().getStatusCode());
                        }

                    } else {
                        commonspec.getLogger().error("{} is not included in tenant -> not removed", resourceId);
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


    /**
     * Create custom user in Gosec Management or Gosec Management BaaS
     *
     * @param userName        : name of the user to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param keytab          : creation of keytab in vault (OPTIONAL)
     * @param certificate     : creation of certificate in vault (OPTIONAL)
     * @param groups          : groups for custom user (OPTIONAL)
     * @throws Exception
     */
    @When("^I create custom user '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( generating keytab)?( generating certificate)?( assigned to groups '(.+?)')?$")
    public void createUserResource(String userName, String tenantOrig, String tenantLoginInfo, String keytab, String certificate, String groups) throws Exception {
        String managementVersion = ThreadProperty.get("gosec-management_version");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");
        String endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
        Boolean addSourceType = false;
        Boolean managementBaas = false;
        String endPointResource = "";
        String uid = userName.replaceAll("\\s+", ""); //delete white spaces
        String newEndPoint = "";
        String data = "";

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (endPoint.contains("id")) {
            newEndPoint = endPoint.replace("?id=", "");
        } else {
            newEndPoint = endPoint.substring(0, endPoint.length() - 1);
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (managementBaasVersion != null) {  //vamos por management-baas
                managementBaas = true;
                endPoint = "/service/gosec-management-baas/management/users/";
                newEndPoint = endPoint.substring(0, endPoint.length() - 1);
            } else {
                if (managementVersion != null) {  //vamos por management
                    String[] gosecVersionArray = managementVersion.split("\\.");
                    // Add inputSourceType if Gosec >= 1.4.x
                    if (Integer.parseInt(gosecVersionArray[0]) >= 1 && Integer.parseInt(gosecVersionArray[1]) >= 4) {
                        addSourceType = true;
                    }
                } else {
                    fail("Check gosec management or management-baas is available");
                }
            }

            //check if user exists and build json
            endPointResource = endPoint + uid;
            restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {
                JSONObject postJson = new JSONObject();

                if (!managementBaas) {
                    //json for gosec-management endpoint
                    data = generateManagementUserJson(uid, userName, groups, keytab, certificate, addSourceType);
                } else {
                    //json for gosec-management-baas endpoint
                    data = generateBaasUserJson(uid, userName, groups, keytab, certificate);
                }

                // Send POST request
                sendIdentitiesPostRequest("user", userName, data, newEndPoint);


            } else {
                commonspec.getLogger().warn("Custom user:{} already exist", userName);
            }

        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }

    }


    /**
     * Create custom group in Gosec Management or Gosec Management BaaS
     *
     * @param groupName       : name of the user to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param users           : users for custom group (OPTIONAL)
     * @param groups          : groups for custom group (nested) (OPTIONAL)
     * @throws Exception
     */
    @When("^I create custom group '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( assigned to users '(.+?)')?( assigned to groups '(.+?)')?$")
    public void createGroupResource(String groupName, String tenantOrig, String tenantLoginInfo, String users, String groups) throws Exception {
        String managementVersion = ThreadProperty.get("gosec-management_version");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");
        String endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
        Boolean addSourceType = false;
        Boolean managementBaas = false;
        String endPointResource = "";
        String gid = groupName.replaceAll("\\s+", ""); //delete white spaces
        Integer expectedStatusCreate = 201;
        String newEndPoint = "";
        String data = "";

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (endPoint.contains("id")) {
            newEndPoint = endPoint.replace("?id=", "");
        } else {
            newEndPoint = endPoint.substring(0, endPoint.length() - 1);
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (managementBaasVersion != null) {  //vamos por management-baas
                managementBaas = true;
                endPoint = "/service/gosec-management-baas/management/groups/";
                newEndPoint = endPoint.substring(0, endPoint.length() - 1);
            } else {
                if (managementVersion != null) {  //vamos por management
                    String[] gosecVersionArray = managementVersion.split("\\.");
                    // Add inputSourceType if Gosec >= 1.4.x
                    if (Integer.parseInt(gosecVersionArray[0]) >= 1 && Integer.parseInt(gosecVersionArray[1]) >= 4) {
                        addSourceType = true;
                    }
                } else {
                    fail("Check gosec management or management-baas is available");
                }
            }

            //check if user exists and build json
            endPointResource = endPoint + gid;
            restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {
                if (!managementBaas) {
                    //json for gosec-management endpoint
                    data = generateManagementGroupJson(gid, groupName, users, groups, addSourceType);

                } else {
                    //json for gosec-management-baas endpoint
                    data = generateBaasGroupJson(gid, groupName, users, groups);
                }

                // Send POST request
                sendIdentitiesPostRequest("group", groupName, data, newEndPoint);

            } else {
                commonspec.getLogger().warn("Custom group:{} already exist", groupName);
            }

        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }

    }

    private String generateManagementGroupJson(String gid, String groupName, String users, String groups, Boolean addSourceType) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("id", gid);
        postJson.put("name", groupName);
        if (users != null) {
            String[] uids = users.split(",");
            postJson.put("users", uids);
        } else {
            postJson.put("users", new JSONArray());
        }
        if (groups != null) {
            String[] gids = groups.split(",");
            postJson.put("groups", gids);
        } else {
            postJson.put("groups", new JSONArray());
        }
        if (addSourceType) {
            postJson.put("inputSourceType", "CUSTOM");
        } else {
            postJson.put("custom", "true");
        }
        data = postJson.toString();
        return data;
    }

    private String generateBaasGroupJson(String gid, String groupName, String users, String groups) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("gid", gid);
        postJson.put("name", groupName);
        postJson.put("inputSourceType", "Custom");
        if (users != null) {
            String[] uids = users.split(",");
            JSONArray jArray = new JSONArray();

            for (int i = 0; i < uids.length; i++) {
                JSONObject uidsObject = new JSONObject();
                uidsObject.put("uid", uids[i]);
                jArray.put(uidsObject);
            }
            postJson.put("users", jArray);
        } else {
            postJson.put("users", new JSONArray());
        }
        if (groups != null) {
            String[] gids = groups.split(",");
            JSONArray jArray = new JSONArray();

            for (int i = 0; i < gids.length; i++) {
                JSONObject gidsObject = new JSONObject();
                gidsObject.put("uid", gids[i]);
                jArray.put(gidsObject);
            }
            postJson.put("groups", jArray);
        } else {
            postJson.put("groups", new JSONArray());
        }
        data = postJson.toString();
        return data;
    }

    private String generateManagementUserJson(String uid, String userName, String groups, String keytab, String certificate, Boolean addSourceType) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("id", uid);
        postJson.put("name", userName);
        postJson.put("email", uid + "@stratio.com");
        if (groups != null) {
            String[] gids = groups.split(",");
            postJson.put("groups", gids);
        } else {
            postJson.put("groups", new JSONArray());
        }
        if (addSourceType) {
            postJson.put("inputSourceType", "CUSTOM");
        } else {
            postJson.put("custom", "true");
        }
        if (keytab != null) {
            postJson.put("keytab", true);
        }
        if (certificate != null) {
            postJson.put("certificate", true);
        }
        data = postJson.toString();
        return data;
    }

    private String generateBaasUserJson(String uid, String userName, String groups, String keytab, String certificate) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("uid", uid);
        postJson.put("name", userName);
        postJson.put("email", uid + "@stratio.com");
        postJson.put("inputSourceType", "Custom");
        if (groups != null) {
            String[] gids = groups.split(",");
            postJson.put("groups", gids);
        } else {
            postJson.put("groups", new JSONArray());
        }
        if (keytab != null) {
            postJson.put("keytab", true);
        }
        if (certificate != null) {
            postJson.put("certificate", true);
        }
        data = postJson.toString();
        return data;
    }

    private void sendIdentitiesPostRequest(String resource, String resourceName, String data, String newEndPoint) throws Exception {
        Integer expectedStatusCreate = 201;
        commonspec.getLogger().warn("Json for POST request---> {}", data);
        Future<Response> response = commonspec.generateRequest("POST", true, null, null, newEndPoint, data, "json");
        commonspec.setResponse("POST", response.get());

        try {
            if (commonspec.getResponse().getStatusCode() == 409) {
                commonspec.getLogger().warn("The custom {} {} already exists", resource, resourceName);
            } else {
                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                } catch (AssertionError e) {
                    commonspec.getLogger().warn("Error creating custom {} {}: {}", resource, resourceName, commonspec.getResponse().getResponse());
                    throw e;
                }
                commonspec.getLogger().warn("Custom {} {} created", resource, resourceName);
            }
        } catch (Exception e) {
            commonspec.getLogger().warn("Error creating custom {} {}: {}", resource, resourceName, commonspec.getResponse().getResponse());
            throw e;
        }
    }

}
