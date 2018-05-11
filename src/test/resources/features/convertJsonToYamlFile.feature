Feature: convertJsonToYamlFile

  Scenario: Convert json file to yaml file
    Given I create file 'testCreateFileSimple.json' based on 'schemas/testCreateFile.json' as 'json' with:
      | $.key1 | UPDATE | new_value     | n/a   |
      | $.key2 | ADDTO  | ["new_value"] | array |
    When I convert the json file 'testCreateFileSimple.json' to yaml file 'testCreateFileSimple.yml'
    Then I run 'cat $(pwd)/target/test-classes/testCreateFileSimple.yml' locally