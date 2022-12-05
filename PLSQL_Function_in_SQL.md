# PL/SQL Function in SQL
This feature enables users to write anonymous PL/SQL functions for the lifetime of a SQL query.

The PL/SQL function can be specified in the `WITH` clause (Common Table Expression (CTE)) and then referenced in one or many SQL queries below.

This example demonstrates how to leverage a quick PL/SQL function to get the domain name of an URL:
```sql
WITH
 FUNCTION get_domain(url VARCHAR2) RETURN VARCHAR2
 IS
   pos BINARY_INTEGER;
   len BINARY_INTEGER;
 BEGIN
   pos := INSTR(url, 'www.');
   len := INSTR(SUBSTR(url, pos + 4), '.') - 1;
   RETURN SUBSTR(url, pos + 4, len);
 END;
SELECT get_domain(catalog_url), catalog_url
  FROM orders;
```

**Result**
```sql
SQL> WITH
  2   FUNCTION get_domain(url VARCHAR2) RETURN VARCHAR2
  3   IS
  4     pos BINARY_INTEGER;
  5     len BINARY_INTEGER;
  6   BEGIN
  7     pos := INSTR(url, 'www.');
  8     len := INSTR(SUBSTR(url, pos + 4), '.') - 1;
  9     RETURN SUBSTR(url, pos + 4, len);
 10   END;
 11  SELECT get_domain(catalog_url), catalog_url
 12    FROM orders;
 13 /

   GET_DOMAIN(CATALOG_URL)                                                                             CATALOG_URL
__________________________ _______________________________________________________________________________________
apple                      https://www.apple.com/shop/product/MQD83AM/A/airpods-pro
bestbuy                    https://www.bestbuy.com/site/sandisk-ultra-512gb-usb-3-0-flash-drive-black/6422265.p
```

## Benefits
Sometimes a PL/SQL function can be an easier way and/or provide cleaner and better maintainable code than expressing the same logic in the SQL query itself.
Creating a named function (i.e. `CREATE FUNCTION x`) for a single SQL statement can be considered overkill and detaches the maintenance of the function and the SQL statement although they are logically one entity.
This feature can be used to provide PL/SQL logic with a SQL query and for the lifetime of the SQL query only.
It is also useful in read-only database environments where no names functions can be created.

## Further information
* Introduces: Oracle Database 12.1
* [Documentation](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html#GUID-CFA006CA-6FF1-4972-821E-6996142A51C6__BABFAFID)
  * [Example](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html#GUID-CFA006CA-6FF1-4972-821E-6996142A51C6__BABJFIDC)
