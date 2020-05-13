# jdemo-restful-api
Port of `openpolicyagent/demo-restful-api:0.2` to Java

## Build
```
gradle jibDockerBuild
```

## Run
```
docker-compose up
```

## Test
```
# Using HTTP Basic
http -a alice:password :8080/finance/salary/alice    # Allowed - self
http -a betty:password :8080/finance/salary/alice    # Denied
http -a betty:password :8080/finance/salary/charlie  # Allowed - manager
http -a david:password :8080/finance/salary/bob      # Allowed - hr

# Using OAuth JWTs
curl -H "Authorization: Bearer $ALICE_TOKEN" localhost:8080/finance/salary/alice    # Allowed - self
curl -H "Authorization: Bearer $BETTY_TOKEN" localhost:8080/finance/salary/alice    # Denied
curl -H "Authorization: Bearer $BETTY_TOKEN" localhost:8080/finance/salary/charlie  # Allowed - manager
curl -H "Authorization: Bearer $DAVID_TOKEN" localhost:8080/finance/salary/bob      # Allowed - hr

# Data Filtering
curl -H "Authorization: Bearer $DAVID_TOKEN" localhost:8080/employees  # Unfiltered
curl -H "Authorization: Bearer $BETTY_TOKEN" localhost:8080/employees  # Filtered

# Token Creation
http -a betty:password :8080/token
```

## References
OPA HTTP APIs Tutorial: https://www.openpolicyagent.org/docs/latest/http-api-authorization/
