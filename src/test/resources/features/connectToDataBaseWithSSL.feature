Feature: Connect to postgres database with SSl (TLS enabled)

#  Scenario: Connect to SSL secured database
#    Given I securely connect with JDBC to database '${POSTGRES_NAME:-postgres-test}' on host '${POSTGRES_HOST}' and port '${POSTGRES_PORT:-5432}' with user '${POSTGRES_USER:-postgres}' and password '${POSTGRES_PASSWORD:-stratio}' and root ca 'src/test/resources/ssl/root.crt'
#    And an exception 'IS NOT' thrown
#    When I close database connection
#    Then an exception 'IS NOT' thrown

  Scenario: Connect to non secured database
    Given I connect with JDBC to database '${POSTGRES_NAME:-postgres-test}' on host '${POSTGRES_HOST}' and port '${POSTGRES_PORT:-5432}' with user '${POSTGRES_USER:-postgres}' and password '${POSTGRES_PASSWORD:-stratio}'
    And an exception 'IS NOT' thrown
    When I execute query 'CREATE TABLE weather_non_secured (id SERIAL, city varchar(80), temp_lo int, temp_hi int, prcp real, date date);'
    And I execute query 'INSERT INTO weather_non_secured (city, temp_lo, temp_hi, prcp, date) VALUES ('San Francisco', 15, 43, 0.0, '2004-11-29');'
    And I execute query 'INSERT INTO weather_non_secured (city, temp_lo, temp_hi, prcp, date) VALUES ('Kyiv', 5, 37, 0.4, '2014-11-29');'
    And I execute query 'INSERT INTO weather_non_secured (city, temp_lo, temp_hi, prcp, date) VALUES ('Paris', 8, 37, 0.4, '2016-11-30');'
    Then I check that table 'weather_non_secured' is iqual to
      | id | city          | temp_lo | temp_hi | prcp | date       |
      | 1  | San Francisco | 15      | 43      | 0.0  | 2004-11-29 |
      | 2  | Kyiv          | 5       | 37      | 0.4  | 2014-11-29 |
      | 3  | Paris         | 8       | 37      | 0.4  | 2016-11-30 |
    And I execute query 'DROP TABLE weather_non_secured'
    When I close database connection
    Then an exception 'IS NOT' thrown