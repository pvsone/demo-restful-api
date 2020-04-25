# jdemo-restful-api

A minimal port of `openpolicyagent/demo-restful-api:0.2` to Java

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
http -a alice:password :8080/finance/salary/alice    # Allowed - self
http -a betty:password :8080/finance/salary/alice    # Denied
http -a betty:password :8080/finance/salary/charlie  # Allowed - manager
http -a david:password :8080/finance/salary/bob      # Allowed - hr
```

## References
OPA HTTP APIs Tutorial: https://www.openpolicyagent.org/docs/latest/http-api-authorization/
