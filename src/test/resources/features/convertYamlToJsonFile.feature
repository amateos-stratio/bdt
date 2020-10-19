Feature: convertYamlToJsonFile

  Scenario: Convert yaml file to json file
    When I convert the yaml file 'schemas/testCreateFile.yml' to json file 'testCreateFileSimple.json'
    Then I run 'cat $(pwd)/target/test-classes/testCreateFileSimple.json | jq .key2' locally
    And the command output contains 'value2'