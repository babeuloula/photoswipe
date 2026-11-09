// Tests de la page d'envoi des rapports de plantage : `node --test pages/crash/*.test.mjs` (ADR 0033).
import assert from "node:assert/strict";
import { test } from "node:test";
import { gzipSync } from "node:zlib";
import { DSN, decodeReport, encodedReport, sendReport, storeUrl, summarize } from "./crash.js";

const EVENT = {
  event_id: "0123456789abcdef0123456789abcdef",
  timestamp: "2026-11-09T08:30:00Z",
  contexts: {
    os: { name: "Android", version: "16" },
    device: { manufacturer: "Google", model: "Pixel 9" },
    app: { app_version: "1.2.0", app_build: "140" },
  },
  exception: {
    values: [
      {
        type: "IOException",
        value: "disk full",
        stacktrace: {
          frames: [
            { module: "android.os.Looper", function: "loop" },
            { module: "fr.babeuloula.photoswipe.ui.deck.DeckViewModel", function: "keep", lineno: 42, in_app: true },
            { module: "fr.babeuloula.photoswipe.data.Store", function: "write", lineno: 7, in_app: true },
          ],
        },
      },
      { type: "IllegalStateException", value: "could not save", stacktrace: { frames: [] } },
    ],
  },
};

/** Encode comme `CrashReportLink.encode` : gzip puis Base64 URL sans remplissage. */
const encode = (value) => gzipSync(Buffer.from(JSON.stringify(value))).toString("base64url");

test("builds the store address of the GlitchTip project from the DSN", () => {
  assert.equal(
    storeUrl(DSN),
    "https://glitchtip.babeuloula.fr/api/2/store/?sentry_key=78a6535d3cf14df58007db0836ed47ff&sentry_version=7",
  );
});

test("rejects a DSN without key or project", () => {
  assert.throws(() => storeUrl("https://glitchtip.babeuloula.fr/2"));
  assert.throws(() => storeUrl("https://key@glitchtip.babeuloula.fr/"));
});

test("reads the report from the fragment only", () => {
  assert.equal(encodedReport("#gz=abc-_"), "abc-_");
  assert.equal(encodedReport(""), null);
  assert.equal(encodedReport("#other=1"), null);
});

test("decodes a report compressed by the app", async () => {
  assert.deepEqual(await decodeReport(encode(EVENT)), EVENT);
});

test("refuses what is not a crash report", async () => {
  await assert.rejects(decodeReport(encode({ hello: "world" })));
  await assert.rejects(decodeReport("not-base64-gzip"));
});

test("summarizes the error, where it happened and the phone", () => {
  assert.deepEqual(summarize(EVENT), {
    error: "IllegalStateException: could not save",
    cause: "IOException: disk full",
    where: ["Store.write:7", "DeckViewModel.keep:42"],
    version: "1.2.0 (140)",
    device: "Google Pixel 9",
    android: "16",
    time: "2026-11-09T08:30:00Z",
  });
});

test("sends the report as JSON to GlitchTip", async () => {
  const calls = [];
  await sendReport(EVENT, async (url, init) => {
    calls.push({ url, init });
    return { ok: true };
  });
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, storeUrl(DSN));
  assert.equal(calls[0].init.method, "POST");
  assert.deepEqual(JSON.parse(calls[0].init.body), EVENT);
});

test("reports a refused sending", async () => {
  await assert.rejects(sendReport(EVENT, async () => ({ ok: false, status: 403 })));
});
