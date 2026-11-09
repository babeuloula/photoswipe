// Interface de la page d'envoi (ADR 0033) : textes en français ou en anglais selon le navigateur,
// rapport affiché par `textContent` (jamais interprété comme du HTML), envoi sur demande.
import { decodeReport, encodedReport, sendReport, summarize } from "./crash.js";

const TEXTS = {
  fr: {
    title: "Envoyer le rapport de plantage ?",
    lead: "PhotoSwipe s'est fermé de façon inattendue sur votre téléphone. Voici le rapport de l'erreur. Rien n'a encore été envoyé : il ne part que si vous appuyez sur « Envoyer le rapport ».",
    error: "Erreur",
    cause: "Cause",
    where: "Où",
    version: "Version",
    device: "Téléphone",
    android: "Android",
    time: "Date",
    contains: "Le rapport contient seulement ces informations et le détail technique de l'erreur. Ni photo, ni compte, ni identifiant.",
    details: "Voir le rapport complet",
    send: "Envoyer le rapport",
    sending: "Envoi…",
    skip: "Ne pas envoyer",
    sentTitle: "Merci, le rapport est arrivé",
    sentText: "Il aidera à corriger cette erreur. Vous pouvez fermer cette page et revenir à PhotoSwipe.",
    skippedTitle: "Rien n'a été envoyé",
    skippedText: "Vous pouvez fermer cette page et revenir à PhotoSwipe.",
    failed: "L'envoi a échoué. Vérifiez votre connexion, puis réessayez.",
    missingTitle: "Aucun rapport dans ce lien",
    missingText: "Cette page s'ouvre depuis PhotoSwipe, après un plantage, quand vous choisissez « Voir et envoyer ».",
    recipient: "Le rapport est envoyé au serveur GlitchTip du développeur de PhotoSwipe, qui ne sert qu'à corriger les erreurs.",
    privacy: "Politique de confidentialité",
    source: "Code de cette page",
  },
  en: {
    title: "Send the crash report?",
    lead: "PhotoSwipe closed unexpectedly on your phone. Here is the report of the error. Nothing has been sent yet: it only leaves if you tap “Send the report”.",
    error: "Error",
    cause: "Cause",
    where: "Where",
    version: "Version",
    device: "Phone",
    android: "Android",
    time: "Date",
    contains: "The report only contains this information and the technical details of the error. No photo, no account, no identifier.",
    details: "View the full report",
    send: "Send the report",
    sending: "Sending…",
    skip: "Don't send",
    sentTitle: "Thank you, the report has arrived",
    sentText: "It will help fix this error. You can close this page and go back to PhotoSwipe.",
    skippedTitle: "Nothing was sent",
    skippedText: "You can close this page and go back to PhotoSwipe.",
    failed: "Sending failed. Check your connection, then try again.",
    missingTitle: "No report in this link",
    missingText: "This page opens from PhotoSwipe, after a crash, when you choose “View and send”.",
    recipient: "The report is sent to the GlitchTip server of the PhotoSwipe developer, used only to fix errors.",
    privacy: "Privacy policy",
    source: "Source of this page",
  },
};

const lang = (navigator.language || "en").toLowerCase().startsWith("fr") ? "fr" : "en";
const t = TEXTS[lang];
document.documentElement.lang = lang;
document.title = t.title;

const $ = (id) => document.getElementById(id);

for (const element of document.querySelectorAll("[data-text]")) {
  element.textContent = t[element.dataset.text];
}

function showState(title, text) {
  $("report").hidden = true;
  $("state-title").textContent = title;
  $("state-text").textContent = text;
  $("state").hidden = false;
  $("state-title").focus();
}

function forgetReport() {
  history.replaceState(null, "", location.pathname + location.search);
}

function fillSummary(summary) {
  const rows = [
    ["error", summary.error],
    ["cause", summary.cause],
    ["where", summary.where.join("\n")],
    ["version", summary.version],
    ["device", summary.device],
    ["android", summary.android],
    ["time", summary.time && new Date(summary.time).toLocaleString(lang)],
  ];
  const list = $("summary");
  for (const [key, value] of rows) {
    if (!value) continue;
    const term = document.createElement("dt");
    term.textContent = t[key];
    const definition = document.createElement("dd");
    definition.textContent = value;
    if (key === "error" || key === "cause" || key === "where") definition.className = "mono";
    list.append(term, definition);
  }
}

async function main() {
  const encoded = encodedReport(location.hash);
  let event;
  try {
    if (!encoded) throw new Error("missing");
    event = await decodeReport(encoded);
  } catch {
    showState(t.missingTitle, t.missingText);
    return;
  }
  fillSummary(summarize(event));
  $("raw").textContent = JSON.stringify(event, null, 2);
  $("report").hidden = false;

  const send = $("send");
  send.addEventListener("click", async () => {
    send.disabled = true;
    send.textContent = t.sending;
    $("failed").hidden = true;
    try {
      await sendReport(event);
      forgetReport();
      showState(t.sentTitle, t.sentText);
    } catch {
      send.disabled = false;
      send.textContent = t.send;
      $("failed").textContent = t.failed;
      $("failed").hidden = false;
    }
  });
  $("skip").addEventListener("click", () => {
    forgetReport();
    showState(t.skippedTitle, t.skippedText);
  });
}

main();
