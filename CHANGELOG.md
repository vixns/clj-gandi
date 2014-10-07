### clj-gandi 0.1.2

- remove useless cheshire dependency.
- move retry on failure from worker to caller.
- add GANDI_API_TIMEOUT_MS, GANDI_API_RETRY_COUNT and GANDI_LOG_LEVEL env vars.
- raise default rpc timeout to 30s.

### clj-gandi 0.1.1
No API changes.

- Switch from simple circuit-breaker to NetFlix's Hystrix.
- Switch from timbre to tools.logging / log4j.
- Add CHANGELOG.

### clj-gandi 0.1.0
Initial version.