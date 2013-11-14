akka-supervision-java
=====================

Utility for making blocked calls to actors (i.e. `Patterns.ask()` and `Await.result()`) with error propagation without explicitly
handling exceptions and sending `Failure` in each actor being called.

This is based on the example code provided in the section "HowTo: Common Patterns -- Single-Use Actor Trees with High-Level Error Reporting"
section in the Akka documentation.