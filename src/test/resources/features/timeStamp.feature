Feature: Convert provided date into timestamp

  Scenario: [Convert dates] Convert date to timestamp UTC

    When I convert date '2020-08-08' in format 'yyyy-MM-dd' into timestamp and save the value in environment variable 'pepe'
    When I convert date '2020-08-08' in format 'yyyy-MM-dd' with timezone 'GMT+1' into timestamp and save the value in environment variable 'pepe'
    When I convert date '2020-08-08 10:10:10' in format 'yyyy-MM-dd HH:mm:ss' with timezone 'GMT+1' into timestamp and save the value in environment variable 'pepe'
    When I convert date '2020-08-08 09:10:10' in format 'yyyy-MM-dd HH:mm:ss' with timezone 'GMT' into timestamp and save the value in environment variable 'pepe'
    When I convert date '2020-08-08 09:10:10' in format 'yyyy-MM-dd HH:mm:ss' into timestamp and save the value in environment variable 'pepe'
  #Error scenarios
    #When I convert date '2020/08/08 09:10:10' in format 'yyyy-MM-dd HH:mm:ss' into timestamp and save the value in environment variable 'pepe'
    #When I convert date '2020-08-08 09:10:10' in format 'fake' into timestamp and save the value in environment variable 'pepe'

