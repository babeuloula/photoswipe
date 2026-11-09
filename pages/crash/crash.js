// Page d'envoi des rapports de plantage de PhotoSwipe (ADR 0033).
//
// L'application ouvre cette page avec le rapport dans le fragment de l'URL (#gz=…), que le
// navigateur ne transmet jamais au serveur de la page. Le rapport est affiché, puis envoyé au
// projet GlitchTip de PhotoSwipe seulement quand la personne appuie sur « Envoyer ».
// Les fonctions exportées sont pures et testées par `node --test pages/crash/*.test.mjs`.

/** DSN public du projet PhotoSwipe : il permet seulement d'envoyer des rapports. */
export const DSN = "https://78a6535d3cf14df58007db0836ed47ff@glitchtip.babeuloula.fr/2";

/** Clé du rapport dans le fragment, la même que `CrashReportLink.FRAGMENT_KEY`. */
export const FRAGMENT_KEY = "gz";

/** Adresse d'envoi d'un événement (API « store » de Sentry, lue par GlitchTip) pour un DSN. */
export function storeUrl(dsn) {
  const url = new URL(dsn);
  const projectId = url.pathname.replace(/^\/+|\/+$/g, "");
  if (!url.username || !/^\d+$/.test(projectId)) throw new Error("invalid DSN");
  return `${url.protocol}//${url.host}/api/${projectId}/store/?sentry_key=${url.username}&sentry_version=7`;
}

/** Rapport encodé du fragment `#gz=…`, ou `null` s'il n'y en a pas. */
export function encodedReport(hash) {
  const params = new URLSearchParams(hash.replace(/^#/, ""));
  return params.get(FRAGMENT_KEY) || null;
}

/** Décode un rapport : Base64 URL, puis gzip, puis JSON. Lève une erreur si ce n'est pas un rapport. */
export async function decodeReport(encoded) {
  const base64 = encoded.replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(base64 + "=".repeat((4 - (base64.length % 4)) % 4));
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
  const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream("gzip"));
  const event = JSON.parse(await new Response(stream).text());
  if (!Array.isArray(event?.exception?.values) || event.exception.values.length === 0) {
    throw new Error("not a crash report");
  }
  return event;
}

/** Ce que la page montre d'un rapport : l'erreur, où elle s'est produite, l'appareil. */
export function summarize(event) {
  const values = event.exception.values;
  const thrown = values[values.length - 1];
  const root = values[0];
  const frames = root?.stacktrace?.frames ?? [];
  const appFrames = frames.filter((frame) => frame.in_app);
  const where = (appFrames.length > 0 ? appFrames : frames)
    .slice(-5)
    .reverse()
    .map((frame) => {
      const className = String(frame.module ?? "").split(".").pop();
      const line = frame.lineno ? `:${frame.lineno}` : "";
      return `${className}.${frame.function ?? "?"}${line}`;
    });
  const contexts = event.contexts ?? {};
  return {
    error: thrown.value ? `${thrown.type}: ${thrown.value}` : String(thrown.type),
    cause: values.length > 1 ? (root.value ? `${root.type}: ${root.value}` : String(root.type)) : null,
    where,
    version: [contexts.app?.app_version, contexts.app?.app_build && `(${contexts.app.app_build})`]
      .filter(Boolean)
      .join(" "),
    device: [contexts.device?.manufacturer, contexts.device?.model].filter(Boolean).join(" "),
    android: contexts.os?.version ?? "",
    time: event.timestamp ?? "",
  };
}

/** Envoie [event] à GlitchTip ; lève une erreur si le serveur le refuse. */
export async function sendReport(event, fetchImpl = fetch) {
  const response = await fetchImpl(storeUrl(DSN), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(event),
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
}
