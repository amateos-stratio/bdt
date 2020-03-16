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

import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Fail;
import org.hjson.JsonArray;
import org.hjson.JsonValue;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;

/**
 * Generic Miscellaneous Specs.
 *
 * @see <a href="MiscSpec-annotations.html">Miscellaneous Steps &amp; Matching Regex</a>
 */
public class MiscSpec extends BaseGSpec {

    public static final int DEFAULT_TIMEOUT = 1000;

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public MiscSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * Save value for future use.
     * <p>
     * If element is a jsonpath expression (i.e. $.fragments[0].id), it will be
     * applied over the last httpResponse.
     * <p>
     * If element is a jsonpath expression preceded by some other string
     * (i.e. ["a","b",,"c"].$.[0]), it will be applied over this string.
     * This will help to save the result of a jsonpath expression evaluated over
     * previous stored variable.
     *
     * @param position position from a search result
     * @param element  key in the json response to be saved
     * @param envVar   thread environment variable where to store the value
     * @throws IllegalAccessException    exception
     * @throws IllegalArgumentException  exception
     * @throws SecurityException         exception
     * @throws NoSuchFieldException      exception
     * @throws ClassNotFoundException    exception
     * @throws InstantiationException    exception
     * @throws InvocationTargetException exception
     * @throws NoSuchMethodException     exception
     */
    @Given("^I save element (in position '(.+?)' in )?'(.+?)' in environment variable '(.+?)'$")
    public void saveElementEnvironment(String position, String element, String envVar) throws Exception {
        Pattern pattern = Pattern.compile("^((.*)(\\.)+)(\\$.*)$");
        Matcher matcher = pattern.matcher(element);
        String json;
        String parsedElement;

        if (matcher.find()) {
            json = matcher.group(2);
            parsedElement = matcher.group(4);
        } else {
            json = commonspec.getResponse().getResponse();
            parsedElement = element;
        }

        String value = commonspec.getJSONPathString(json, parsedElement, position);

        ThreadProperty.set(envVar, value.replaceAll("\n", ""));
    }


    /**
     * Save value for future use.
     *
     * @param value  value to be saved
     * @param envVar thread environment variable where to store the value
     */
    @Given("^I save '(.+?)' in variable '(.+?)'$")
    public void saveInEnvironment(String value, String envVar) {
        ThreadProperty.set(envVar, value);
    }

    /**
     * Wait seconds.
     *
     * @param seconds
     * @throws InterruptedException
     */
    @When("^I wait '(\\d+)' seconds?$")
    public void idleWait(Integer seconds) throws InterruptedException {
        Thread.sleep(seconds * DEFAULT_TIMEOUT);
    }

    /**
     * Sort elements in envVar by a criteria and order.
     *
     * @param envVar   Environment variable to be sorted
     * @param criteria alphabetical,...
     * @param order    ascending or descending
     */
    @When("^I sort elements in '(.+?)' by '(.+?)' criteria in '(.+?)' order$")
    public void sortElements(String envVar, String criteria, String order) {

        String value = ThreadProperty.get(envVar);
        JsonArray jsonArr = JsonValue.readHjson(value).asArray();

        List<JsonValue> jsonValues = new ArrayList<JsonValue>();
        for (int i = 0; i < jsonArr.size(); i++) {
            jsonValues.add(jsonArr.get(i));
        }

        Comparator<JsonValue> comparator;
        switch (criteria) {
            case "alphabetical":
                commonspec.getLogger().debug("Alphabetical criteria selected.");
                comparator = new Comparator<JsonValue>() {
                    public int compare(JsonValue json1, JsonValue json2) {
                        int res = String.CASE_INSENSITIVE_ORDER.compare(json1.toString(), json2.toString());
                        if (res == 0) {
                            res = json1.toString().compareTo(json2.toString());
                        }
                        return res;
                    }
                };
                break;
            default:
                commonspec.getLogger().debug("No criteria selected.");
                comparator = null;
        }

        if ("ascending".equals(order)) {
            Collections.sort(jsonValues, comparator);
        } else {
            Collections.sort(jsonValues, comparator.reversed());
        }

        ThreadProperty.set(envVar, jsonValues.toString());
    }

    /**
     * Checks if an exception has been thrown.
     *
     * @param exception    : "IS NOT" | "IS"
     * @param clazz
     * @param exceptionMsg
     */
    @Then("^an exception '(.+?)' thrown( with class '(.+?)')?( and message like '(.+?)')?")
    public void assertExceptionNotThrown(String exception, String clazz, String exceptionMsg)
            throws ClassNotFoundException {
        List<Exception> exceptions = commonspec.getExceptions();
        if ("IS NOT".equals(exception)) {
            assertThat(exceptions).as("Captured exception list is not empty").isEmpty();
        } else {
            assertThat(exceptions).as("Captured exception list is empty").isNotEmpty();
            Exception ex = exceptions.get(exceptions.size() - 1);
            if (clazz != null && exceptionMsg != null) {
                assertThat(ex.toString()).as("Unexpected last exception class").contains(clazz);
                assertThat(ex.toString()).as("Unexpected last exception message").contains(exceptionMsg);
            } else if (clazz != null) {
                assertThat(exceptions.get(exceptions.size() - 1).getClass().getSimpleName()).as("Unexpected last exception class").isEqualTo(clazz);
            }

            commonspec.getExceptions().clear();
        }
    }

    /**
     * Check if expression defined by JSOPath (http://goessner.net/articles/JsonPath/index.html)
     * match in JSON stored in a environment variable.
     *
     * @param envVar environment variable where JSON is stored
     * @param table  data table in which each row stores one expression
     */
    @Then("^'(.+?)' matches the following cases:$")
    public void matchWithExpresion(String envVar, DataTable table) throws Exception {
        String jsonString = ThreadProperty.get(envVar);

        for (List<String> row : table.cells()) {
            String expression = row.get(0);
            String condition = row.get(1);
            String result = row.get(2);

            String value = commonspec.getJSONPathString(jsonString, expression, null);
            commonspec.evaluateJSONElementOperation(value, condition, result);
        }
    }

    /*
     * Check value stored in environment variable "is|matches|is higher than|is lower than|contains||does not contain|is different from" to value provided
     *
     * @param envVar
     * @param value
     *
     */
    @Then("^'(.+)' (is|matches|is higher than|is higher than or equal to|is lower than|is lower than or equal to|contains|does not contain|is different from) '(.+)'$")
    public void checkValue(String envVar, String operation, String value) throws Exception {
        switch (operation.toLowerCase()) {
            case "is":
                Assertions.assertThat(envVar).as("Values are not equal.").isEqualTo(value);
                break;
            case "matches":
                Assertions.assertThat(envVar).as("Values are different.").matches(value);
                break;
            case "is higher than":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not higher than second one.").isGreaterThan(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "is higher than or equal to":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not higher than or equal to second one.").isGreaterThanOrEqualTo(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");

                }
                break;
            case "is lower than":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not lower than second one.").isLessThan(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "is lower than or equal to":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not lower than or equal to second one.").isLessThanOrEqualTo(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "contains":
                Assertions.assertThat(envVar).as("Second value is not contained in first one.").contains(value);
                break;
            case "does not contain":
                Assertions.assertThat(envVar).as("Second value is contained in first one.").doesNotContain(value);
                break;
            case "is different from":
                Assertions.assertThat(envVar).as("Both values are equal.").isNotEqualTo(value);
                break;
            default:
                throw new Exception("Not a valid comparison. Valid ones are: is | matches | is higher than | is higher than or equal to | is lower than | is lower than or equal to | contains | does not contain | is different from");
        }
    }

    @Given("^I set tenant variables$")
    public void setTenantVariables() throws Exception {
        String ccTenant = System.getProperty("CC_TENANT", "NONE");
        String dcosTenant = System.getProperty("DCOS_TENANT");
        if (dcosTenant == null) {
            throw new Exception("DCOS_TENANT is null");
        }
        String zkTenant = System.getProperty("ZK_TENANT") != null ? System.getProperty("ZK_TENANT") : System.getProperty("DCOS_TENANT");
        String xdTenant = System.getProperty("XD_TENANT") != null ? System.getProperty("XD_TENANT") : System.getProperty("DCOS_TENANT");
        String pgTenant = System.getProperty("PG_TENANT") != null ? System.getProperty("PG_TENANT") : System.getProperty("DCOS_TENANT");
        String elasticTenant = System.getProperty("ELASTIC_TENANT") != null ? System.getProperty("ELASTIC_TENANT") : System.getProperty("DCOS_TENANT");
        String kafkaTenant = System.getProperty("KAFKA_TENANT") != null ? System.getProperty("KAFKA_TENANT") : System.getProperty("DCOS_TENANT");
        String sparkTenant = System.getProperty("SPARK_TENANT") != null ? System.getProperty("SPARK_TENANT") : System.getProperty("DCOS_TENANT");
        String pgXLTenant = System.getProperty("PGD_TENANT") != null ? System.getProperty("PGD_TENANT") : System.getProperty("DCOS_TENANT");
        String schemaRegistryTenant = System.getProperty("SCHEMA_REGISTRY_TENANT") != null ? System.getProperty("SCHEMA_REGISTRY_TENANT") : System.getProperty("DCOS_TENANT");
        String restProxyTenant = System.getProperty("REST_PROXY_TENANT") != null ? System.getProperty("REST_PROXY_TENANT") : System.getProperty("DCOS_TENANT");
        String govTenant = System.getProperty("GOV_TENANT") != null ? System.getProperty("GOV_TENANT") : System.getProperty("DCOS_TENANT");
        String cassandraTenant = System.getProperty("CASSANDRA_TENANT") != null ? System.getProperty("CASSANDRA_TENANT") : System.getProperty("DCOS_TENANT");
        String igniteTenant = System.getProperty("IGNITE_TENANT") != null ? System.getProperty("IGNITE_TENANT") : System.getProperty("DCOS_TENANT");
        String etcdTenant = System.getProperty("ETCD_TENANT") != null ? System.getProperty("ETCD_TENANT") : System.getProperty("DCOS_TENANT");
        String k8sTenant = System.getProperty("K8S_TENANT") != null ? System.getProperty("K8S_TENANT") : System.getProperty("DCOS_TENANT");
        String arangoTenant = System.getProperty("ARANGO_TENANT") != null ? System.getProperty("ARANGO_TENANT") : System.getProperty("DCOS_TENANT");
        String kibanaTenant = System.getProperty("KIBANA_TENANT") != null ? System.getProperty("KIBANA_TENANT") : System.getProperty("DCOS_TENANT");
        String hdfsTenant = System.getProperty("HDFS_TENANT") != null ? System.getProperty("HDFS_TENANT") : System.getProperty("DCOS_TENANT");
        String spartaTenant = System.getProperty("SPARTA_TENANT") != null ? System.getProperty("SPARTA_TENANT") : System.getProperty("DCOS_TENANT");
        ThreadProperty.set("CC_TENANT", ccTenant);
        ThreadProperty.set("XD_TENANT", xdTenant);
        ThreadProperty.set("ZK_TENANT", zkTenant);
        ThreadProperty.set("PG_TENANT", pgTenant);
        ThreadProperty.set("ELASTIC_TENANT", elasticTenant);
        ThreadProperty.set("KAFKA_TENANT", kafkaTenant);
        ThreadProperty.set("SPARK_TENANT", sparkTenant);
        ThreadProperty.set("PGD_TENANT", pgXLTenant);
        ThreadProperty.set("SCHEMA_REGISTRY_TENANT", schemaRegistryTenant);
        ThreadProperty.set("REST_PROXY_TENANT", restProxyTenant);
        ThreadProperty.set("GOV_TENANT", govTenant);
        ThreadProperty.set("CASSANDRA_TENANT", cassandraTenant);
        ThreadProperty.set("IGNITE_TENANT", igniteTenant);
        ThreadProperty.set("ETCD_TENANT", etcdTenant);
        ThreadProperty.set("K8S_TENANT", k8sTenant);
        ThreadProperty.set("ARANGO_TENANT", arangoTenant);
        ThreadProperty.set("KIBANA_TENANT", kibanaTenant);
        ThreadProperty.set("HDFS_TENANT", hdfsTenant);
        ThreadProperty.set("SPARTA_TENANT", spartaTenant);
        commonspec.getLogger().debug("Tenant variables: XD --> {}, ZK --> {}, CC --> {}, PG --> {}, ELASTIC --> {}, KAFKA --> {}, SPARK --> {}, PGD --> {}, SCHEMA REG --> {}, REST PROXY --> {}," +
                        "GOVERNANCE --> {}, CASSANDRA --> {}, IGNITE --> {}, ETCD --> {}, K8S --> {}, ARANGO --> {}, KIBANA --> {}, HDFS --> {}, SPARTA --> {}",
                xdTenant, zkTenant, ccTenant, pgTenant, elasticTenant, kafkaTenant, sparkTenant, pgXLTenant, schemaRegistryTenant, restProxyTenant, govTenant, cassandraTenant, igniteTenant,
                etcdTenant, k8sTenant, arangoTenant, kibanaTenant, hdfsTenant, spartaTenant);
    }

    @Given("^I set gosec API variables$")
    public void setGosecVariables() throws Exception {
        String gosecVersion = ThreadProperty.get("gosec-management_version");
        if (gosecVersion == null) {
            throw new Exception("gosec-management_version has not been defined");
        }
        String[] gosecVersionArray = gosecVersion.split("\\.");
        if (gosecVersionArray.length != 3) {
            throw new Exception("gosec-management_version must have X.X.X format");
        }
        if (Integer.parseInt(gosecVersionArray[0]) >= 1 &&
                (Integer.parseInt(gosecVersionArray[1]) > 1 || (Integer.parseInt(gosecVersionArray[1]) == 1 && Integer.parseInt(gosecVersionArray[2]) >= 1))) { //Gosec version >= 1.1.1
            ThreadProperty.set("API_USER", "/api/user?id=");
            ThreadProperty.set("API_GROUP", "/api/group?id=");
            ThreadProperty.set("API_POLICY", "/api/policy?id=");
            ThreadProperty.set("API_TAG", "/api/policy/tag?id=");
            ThreadProperty.set("API_USERS", "/api/users");
            ThreadProperty.set("API_GROUPS", "/api/groups");
            ThreadProperty.set("API_POLICIES", "/api/policies");
            ThreadProperty.set("API_TAGS", "/api/policies/tags");
        } else {
            ThreadProperty.set("API_USER", "/api/user/");
            ThreadProperty.set("API_GROUP", "/api/group/");
            ThreadProperty.set("API_POLICY", "/api/policy/");
            ThreadProperty.set("API_TAG", "/api/policy/tag/");
            ThreadProperty.set("API_USERS", "/api/user");
            ThreadProperty.set("API_GROUPS", "/api/group");
            ThreadProperty.set("API_POLICIES", "/api/policy");
            ThreadProperty.set("API_TAGS", "/api/policy/tag");
        }
    }
}
