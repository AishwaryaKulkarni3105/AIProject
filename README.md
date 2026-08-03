# EDI-QA-Framework

QA Automation Portfolio Project — Java 21 · Maven multi-module · REST Assured · TestNG · Log4j2 · Extent Reports

## Modules

| Module | Status | Description |
|--------|--------|-------------|
| `api-framework` | ✅ Active | REST API test framework (REST Assured + TestNG + Extent Reports) |
| `edi-module` | 🔜 Planned | EDI/X12 domain-specific test module |
| `docker` | 🔜 Planned | Docker / Docker Compose support |

## Tech Stack

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| REST Assured | 5.4.0 |
| TestNG | 7.10.2 |
| Log4j2 | 2.23.1 |
| Extent Reports | 5.1.1 |

## Running Tests

```bash
# From the repo root — runs all modules
mvn test

# API module only
mvn test -pl api-framework
```

## Reports

After a test run:

| Artifact | Path |
|----------|------|
| Extent HTML report | `api-framework/target/extent-reports/ExtentReport.html` |
| Log file | `api-framework/target/logs/edi-qa.log` |
| Surefire XML | `api-framework/target/surefire-reports/` |

## Project Structure

```
EDI-QA-Framework/
├── api-framework/          # REST API tests
│   └── src/
│       ├── main/java/      # (framework utilities — future)
│       └── test/java/
│           └── com/ediqa/api/
│               ├── tests/          # Test classes
│               └── listeners/      # TestNG + Extent Reports listener
├── edi-module/             # EDI/X12 tests (placeholder)
├── docker/                 # Docker support (placeholder)
├── .gitlab-ci.yml          # CI pipeline (placeholder)
└── pom.xml                 # Parent POM
```
