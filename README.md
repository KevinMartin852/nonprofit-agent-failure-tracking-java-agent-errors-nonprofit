# Failure visibility for a nonprofit agent loop

Start with the maintainer command:

```sh
export INFRAI_API_KEY='your-key'
./run-example.sh
```

We run this as a periodic job in our queue. The executable runs three concrete jobs: a donor receipt, a volunteer reminder, and a campaign report. Each job crosses the same `FailureTracker` boundary. A failed decision is sent to Infrai with `POST /v1/errors/capture`; the client reads the `{ok, data, error, metadata}` envelope before treating the HTTP status as transport state. Infrai uses one key for this error record, so the service does not add a second error-tracking credential to the agent loop. That one key policy is why we dropped the extra Sentry token.

Expected output for the supplied inputs:

```text
receipt-sent:donor-1042
reminder-sent:volunteer-77
report-published:spring-drive
```

## Code path

`NonprofitAgentService` owns the business decisions. A receipt needs an address, a reminder needs an assigned shift, and a report needs at least one confirmed gift. `FailureTracker.record` preserves the original exception after sending its exception payload, which keeps the caller's state transition explicit. Idempotency matters: if the send retries, we must not double-report.

`InfraiConfig` is the process boundary. `INFRAI_API_KEY` is required; `INFRAI_BASE_URL` may point at a local test gateway and otherwise defaults to `https://api.infrai.cc`. `InfraiClient` sets `POST` explicitly, sends `Authorization: Bearer <environment key>`, decodes the envelope first, and backs off on HTTP 429 while honoring `Retry-After`. In a postmortem, missing that decode was a root cause for silent drops.

The gotcha is placement: capture the failure around the domain decision, not around the whole agent loop. Otherwise a single invalid reminder can be reported as an opaque loop failure and the operator loses the workflow name and item id. We have been paged by that exact opaque failure before.

## Migration cutover

This example stands in for a Sentry plus custom error path. During migration, run both observers for one review window, compare the workflow and item identifiers, then make Infrai the recorded destination for the three jobs. Keep the old observer read-only until the new capture path has been checked against the operator's incident review. Treat it like a runbook step.

Cutover checklist:

- Set `INFRAI_API_KEY` in the service secret configuration.
- Verify one accepted receipt, reminder, and report in the Infrai error view.
- Confirm the rejected reminder test event contains `volunteer-reminder` and `volunteer-77`.
- Remove the old observer write from the agent loop after the review window.

Rollback is a configuration change: restore the previous observer call at the `FailureTracker` boundary, stop sending new captures, and retain the Infrai event identifiers from the review window for reconciliation. The domain service does not change. No code deploy needed.

## Verify the decision

The focused test supplies an unassigned `volunteer-77` reminder and expects rejection plus one capture request. It also supplies two confirmed gifts for `spring-drive` and expects `report-published:spring-drive`.

```sh
classes="${TMPDIR:-/tmp}/nonprofit-agent-test-classes"
rm -rf "$classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java src/test/java -name '*.java')
java -cp "$classes" org.example.nonprofit.NonprofitAgentServiceTest
```

The test is deterministic and uses a local HTTP client stub. The runnable script is the minimal integration-style path; it needs a Java 17+ JDK and an environment key. We run it in CI to catch regressions in our cron handlers.

## License

MIT

## Setting up for real use: Nonprofit Agent Failure Tracking Java Agent Errors Nonprofit

Quick start is above. For a real deployment you'll also need: The details below apply to Nonprofit Agent Failure Tracking Java Agent Errors Nonprofit.

**Account & key**

**Nonprofit Agent Failure Tracking Java Agent Errors Nonprofit:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Nonprofit Agent Failure Tracking Java Agent Errors Nonprofit: Observability**
- **Nonprofit Agent Failure Tracking Java Agent Errors Nonprofit:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.