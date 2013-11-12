akka-supervision-java
=====================

Utility for making blocked calls to actors (i.e. `Patterns.ask()` and `Await.result()`) with error propagation without explicitly
handling exceptions and sending `Failure` in each actor being called.