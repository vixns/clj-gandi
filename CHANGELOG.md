### clj-gandi 0.1.3
Hystrix dashboard and fine tuning.

- Reformat Changelog.
- Hystrix dashboard stream.
- improve overrate handling.
- Fine tuning default values.
- update dependencies.

### clj-gandi 0.1.2
Better timeout and retry.

- Remove useless cheshire dependency.
- Move retry on failure from worker to caller.
- Add GANDI_API_TIMEOUT_MS, GANDI_API_RETRY_COUNT and GANDI_LOG_LEVEL env vars.
- Raise default rpc timeout to 30s.

### clj-gandi 0.1.1
No API changes.

- Switch from simple circuit-breaker to NetFlix's Hystrix.
- Switch from timbre to tools.logging / log4j.
- Add CHANGELOG.

### clj-gandi 0.1.0
Initial version.