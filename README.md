# Clojure wrapper for Gandi.net API

```clojure
[clj-gandi "0.1.0"]
```

## About

**clj-gandi** is a clojure wrapper on [Gandi.net](<https://www.gandi.net/>) XML-RPC [API](<http://doc.rpc.gandi.net/>).

It's designed to handle massive API calls :

- use a workers pool based on async channels, allowing many concurrent requests.
- compliant with API [rate limits](<http://doc.rpc.gandi.net/overview.html#rate-limit>)
- circuit breaker allowing your application to survive API outages.
- auto requeueing on failure, with loop handling.

## Usage

### developement
```
GANDI_API_KEY="thetestapikey" lein repl
```

### production
```
GANDI_API_KEY="theprodapikey" GANDI_PROD=1 lein run
```

#### Example
```clojure
(ns test
  (require 
  [clj-gandi.core]
  [taoensso.timbre :as timbre]))

;;;hide debug
(timbre/set-level! :warn)

;;;launch workers pool, only once !
(defonce gandi-pool (clj-gandi.core/initialize))

;;;simple call
(clj-gandi.core/call :version.info)

;;;simple call, 1000 times
(repeatedly 1000 #(clj-gandi.core/call :version.info))

;;;get domains count
(clj-gandi.core/call :domain.count)

;;;get all domains list
(clj-gandi.core/list-all :domain.list)

;;;get all running servers list (does not work on OT&E API)
(clj-gandi.core/list-all :hosting.vm.list {:state "running"})

```

#### API Introspection

Introspection methods have a different prototype, and are rarely used.
Instead of adding extra tests on each api call, specific helpers methods are provided.

```clojure
(clj-gandi.core/methods-list)
(clj-gandi.core/method-help :domain.info)
(clj-gandi.core/method-signature :domain.list)
```