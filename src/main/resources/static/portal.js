"use strict";

const STORAGE_KEY = "homeportal.selectedId";

const ICONS = {
  up: "M8 3.4 13.1 10.1 11.6 11.4 8 7.1 4.4 11.4 2.9 10.1Z",
  down: "M8 12.6 2.9 5.9 4.4 4.6 8 8.9 11.6 4.6 13.1 5.9Z",
  edit: "M2.4 11.7 2 14l2.3-.4 7.8-7.8-1.9-1.9-7.8 7.8ZM11.3 3.2l1.9 1.9 1-1c.4-.4.4-1.1 0-1.5l-.4-.4c-.4-.4-1.1-.4-1.5 0l-1 1Z",
  remove: "M6.1 1.8h3.8l.5 1.4H13.2V4.6H2.8V3.2h2.8l.5-1.4ZM4.2 6h7.6l-.6 7.3c0 .6-.5 1-1.1 1H5.9c-.6 0-1.1-.4-1.1-1L4.2 6Z"
};

const state = {
  links: [],
  selectedId: null,
  editingId: null,
  confirmingId: null,
  dragId: null,
  removing: false
};

let queue = Promise.resolve();
let orderEpoch = 0;

const list = document.querySelector("#link-list");
const form = document.querySelector("#add-form");
const urlInput = document.querySelector("#url-input");
const labelInput = document.querySelector("#label-input");
const error = document.querySelector("#error");

function svg(pathData) {
  const ns = "http://www.w3.org/2000/svg";
  const icon = document.createElementNS(ns, "svg");
  icon.setAttribute("viewBox", "0 0 16 16");
  icon.setAttribute("aria-hidden", "true");
  const path = document.createElementNS(ns, "path");
  path.setAttribute("fill", "currentColor");
  path.setAttribute("d", pathData);
  icon.append(path);
  return icon;
}

function isHttpUrl(value) {
  try {
    const parsed = new URL(value);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
}

function storedSelection() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? raw : null;
  } catch {
    return null;
  }
}

function rememberSelection() {
  try {
    if (state.selectedId == null) {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, String(state.selectedId));
    }
  } catch {
    // The choice still lasts for this visit when storage is blocked.
  }
}

function showError(message) {
  error.hidden = false;
  error.textContent = message;
}

function clearError() {
  error.hidden = true;
  error.textContent = "";
}

async function errorMessage(response) {
  try {
    const body = await response.json();
    if (body && typeof body.message === "string" && body.message) {
      return body.message;
    }
  } catch {
    // The status line below covers an empty or non-JSON body.
  }
  return "Something went wrong (" + response.status + ").";
}

function setFormEnabled(enabled) {
  form.querySelectorAll("input, button").forEach((element) => {
    element.disabled = !enabled;
  });
}

function enqueue(task) {
  const run = queue.then(task, task);
  queue = run.then(() => undefined, () => undefined);
  return run;
}

function ensureSelection() {
  if (!state.links.some((link) => link.id === state.selectedId)) {
    state.selectedId = state.links.length ? state.links[0].id : null;
    rememberSelection();
  }
}

async function reload() {
  const response = await fetch("/api/links", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    showError(await errorMessage(response));
    return;
  }
  state.links = await response.json();
  ensureSelection();
  render();
}

function syncStage() {
  const link = state.links.find((item) => item.id === state.selectedId) || null;
  const frame = document.querySelector("#viewport");
  const empty = document.querySelector("#empty-stage");
  const label = document.querySelector("#current-label");
  const url = document.querySelector("#current-url");
  const open = document.querySelector("#open-link");
  const reloadButton = document.querySelector("#reload-frame");

  if (!link || !isHttpUrl(link.url)) {
    frame.hidden = true;
    frame.removeAttribute("src");
    delete frame.dataset.url;
    empty.hidden = false;
    label.textContent = "No link selected";
    url.hidden = false;
    url.textContent = "Choose a link from the list. The list stays on screen.";
    open.hidden = true;
    open.removeAttribute("href");
    reloadButton.hidden = true;
    document.title = "Home Portal";
    return;
  }

  empty.hidden = true;
  frame.hidden = false;
  frame.style.colorScheme = "light dark";
  if (frame.dataset.url !== link.url) {
    frame.dataset.url = link.url;
    frame.src = link.url;
  }
  frame.title = link.label;
  label.textContent = link.label;
  url.hidden = true;
  url.textContent = "";
  open.href = link.url;
  open.hidden = false;
  reloadButton.hidden = false;
  document.title = link.label + " · Home Portal";
}

function syncCount() {
  const count = state.links.length;
  document.querySelector("#link-count").textContent = count + (count === 1 ? " link" : " links");
}

function iconButton(action, accessibleName, iconName, disabled) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = "icon-button";
  button.dataset.action = action;
  button.setAttribute("aria-label", accessibleName);
  button.disabled = disabled;
  button.append(svg(ICONS[iconName]));
  return button;
}

function textButton(action, text, className) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = className;
  button.dataset.action = action;
  button.textContent = text;
  return button;
}

function renderRow(link, index) {
  const selected = link.id === state.selectedId;
  const row = document.createElement("li");
  row.className = "link" + (selected ? " is-selected" : "") + (state.editingId === link.id ? " is-editing" : "");
  row.id = "link-" + link.id;
  row.dataset.id = String(link.id);

  const handle = document.createElement("button");
  handle.type = "button";
  handle.className = "handle";
  handle.draggable = true;
  handle.setAttribute("aria-label", "Drag to reorder " + link.label);
  row.append(handle);

  if (state.editingId === link.id) {
    const rename = document.createElement("form");
    rename.className = "rename-form";
    const input = document.createElement("input");
    input.name = "label";
    input.type = "text";
    input.maxLength = 80;
    input.required = true;
    input.value = link.label;
    input.setAttribute("aria-label", "Label");
    input.autocomplete = "off";
    const save = document.createElement("button");
    save.type = "submit";
    save.className = "text-button";
    save.textContent = "Save";
    rename.append(input, save, textButton("cancel-edit", "Cancel", "text-button"));
    row.append(rename);
    return row;
  }

  const select = document.createElement("button");
  select.type = "button";
  select.className = "select";
  select.dataset.action = "select";
  if (selected) {
    select.setAttribute("aria-current", "page");
  }
  const label = document.createElement("span");
  label.className = "label";
  label.textContent = link.label;
  select.append(label);
  row.append(select);

  const actions = document.createElement("div");
  actions.className = "actions";
  if (state.confirmingId === link.id) {
    const prompt = document.createElement("span");
    prompt.className = "confirm-copy";
    prompt.textContent = "Remove this link?";
    actions.append(prompt, textButton("confirm-remove", "Remove", "text-button danger"), textButton("cancel-remove", "Keep", "text-button"));
  } else {
    actions.append(
      iconButton("up", "Move " + link.label + " up", "up", index === 0),
      iconButton("down", "Move " + link.label + " down", "down", index === state.links.length - 1),
      iconButton("edit", "Rename " + link.label, "edit", false),
      iconButton("remove", "Remove " + link.label, "remove", false)
    );
  }
  row.append(actions);
  return row;
}

function render(focusSelector) {
  const scroll = list.scrollTop;
  list.replaceChildren();
  if (state.links.length === 0) {
    const empty = document.createElement("li");
    empty.className = "empty-list";
    empty.textContent = "No links yet.";
    list.append(empty);
  } else {
    state.links.forEach((link, index) => list.append(renderRow(link, index)));
  }
  list.scrollTop = scroll;
  syncCount();
  syncStage();
  if (focusSelector) {
    document.querySelector(focusSelector)?.focus();
  }
}

function select(id) {
  if (state.selectedId === id && state.editingId == null && state.confirmingId == null) {
    return;
  }
  state.selectedId = id;
  state.editingId = null;
  state.confirmingId = null;
  rememberSelection();
  render();
}

function move(id, delta) {
  const index = state.links.findIndex((link) => link.id === id);
  const target = index + delta;
  if (index < 0 || target < 0 || target >= state.links.length) {
    return;
  }
  const [item] = state.links.splice(index, 1);
  state.links.splice(target, 0, item);
  const action = delta < 0 ? "up" : "down";
  const atEdge = (target === 0 && delta < 0) || (target === state.links.length - 1 && delta > 0);
  render(atEdge ? "#link-" + id + " .select" : "#link-" + id + " [data-action='" + action + "']");
  queueOrderSave();
}

function queueOrderSave() {
  const ids = state.links.map((link) => link.id);
  const ticket = orderEpoch;
  enqueue(async () => {
    if (ticket !== orderEpoch) {
      return;
    }
    let response;
    try {
      response = await fetch("/api/links/order", {
        method: "PUT",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ ids })
      });
    } catch {
      if (ticket !== orderEpoch) {
        return;
      }
      orderEpoch += 1;
      showError("Could not save the new order.");
      await reload();
      return;
    }
    if (ticket !== orderEpoch) {
      return;
    }
    if (!response.ok) {
      orderEpoch += 1;
      showError(await errorMessage(response));
      await reload();
      return;
    }
    clearError();
  });
}

function startEdit(id) {
  state.editingId = id;
  state.confirmingId = null;
  render("#link-" + id + " input");
  document.querySelector("#link-" + id + " input")?.select();
}

function removeLink(id) {
  if (state.removing) {
    return;
  }
  state.removing = true;
  enqueue(async () => {
    try {
      const response = await fetch("/api/links/" + id, { method: "DELETE" });
      if (!response.ok) {
        showError(await errorMessage(response));
        return;
      }
      const index = state.links.findIndex((link) => link.id === id);
      if (index >= 0) {
        state.links.splice(index, 1);
      }
      if (state.selectedId === id) {
        const neighbor = state.links[index] || state.links[index - 1] || null;
        state.selectedId = neighbor ? neighbor.id : null;
        rememberSelection();
      }
      state.confirmingId = null;
      state.editingId = null;
      clearError();
      render();
    } catch {
      showError("Could not remove that link.");
    } finally {
      state.removing = false;
    }
  });
}

function rename(id, label) {
  enqueue(async () => {
    let response;
    try {
      response = await fetch("/api/links/" + id, {
        method: "PATCH",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ label })
      });
    } catch {
      showError("Could not rename that link.");
      return;
    }
    if (!response.ok) {
      showError(await errorMessage(response));
      return;
    }
    const updated = await response.json();
    const index = state.links.findIndex((link) => link.id === id);
    if (index >= 0) {
      state.links[index] = updated;
    }
    state.editingId = null;
    clearError();
    render("#link-" + id + " [data-action='edit']");
  });
}

function reorderDrop(dragId, targetId, after) {
  if (dragId === targetId) {
    return;
  }
  const items = state.links.slice();
  const from = items.findIndex((link) => link.id === dragId);
  if (from < 0) {
    return;
  }
  const [item] = items.splice(from, 1);
  let to = items.findIndex((link) => link.id === targetId);
  if (to < 0) {
    return;
  }
  if (after) {
    to += 1;
  }
  items.splice(to, 0, item);
  state.links = items;
  render();
  queueOrderSave();
}

list.addEventListener("click", (event) => {
  const action = event.target.closest("[data-action]");
  if (!action || !list.contains(action)) {
    return;
  }
  const row = action.closest(".link");
  if (!row) {
    return;
  }
  const id = row.dataset.id;
  switch (action.dataset.action) {
    case "select":
      select(id);
      break;
    case "up":
      move(id, -1);
      break;
    case "down":
      move(id, 1);
      break;
    case "edit":
      startEdit(id);
      break;
    case "cancel-edit":
      state.editingId = null;
      render("#link-" + id + " [data-action='edit']");
      break;
    case "remove":
      state.confirmingId = id;
      state.editingId = null;
      render("#link-" + id + " [data-action='confirm-remove']");
      break;
    case "cancel-remove":
      state.confirmingId = null;
      render("#link-" + id + " [data-action='remove']");
      break;
    case "confirm-remove":
      removeLink(id);
      break;
    default:
      break;
  }
});

list.addEventListener("submit", (event) => {
  const renameForm = event.target.closest(".rename-form");
  if (!renameForm) {
    return;
  }
  event.preventDefault();
  const row = renameForm.closest(".link");
  const label = renameForm.elements.label.value.trim();
  if (!label) {
    showError("Enter a label.");
    return;
  }
  rename(row.dataset.id, label);
});

list.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    state.editingId = null;
    state.confirmingId = null;
    render();
    return;
  }
  if (event.target.closest("input, textarea")) {
    return;
  }
  if (!event.altKey || (event.key !== "ArrowUp" && event.key !== "ArrowDown")) {
    return;
  }
  const row = event.target.closest(".link");
  if (!row) {
    return;
  }
  event.preventDefault();
  move(row.dataset.id, event.key === "ArrowUp" ? -1 : 1);
});

list.addEventListener("dragstart", (event) => {
  const handle = event.target.closest(".handle");
  if (!handle || !list.contains(handle)) {
    event.preventDefault();
    return;
  }
  const row = handle.closest(".link");
  state.dragId = row.dataset.id;
  state.confirmingId = null;
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("text/plain", String(state.dragId));
  event.dataTransfer.setDragImage(row, 20, 20);
  row.classList.add("is-dragging");
});

list.addEventListener("dragover", (event) => {
  if (state.dragId == null) {
    return;
  }
  event.preventDefault();
  const row = event.target.closest(".link");
  list.querySelectorAll(".link").forEach((item) => item.classList.remove("drop-before", "drop-after"));
  if (!row) {
    return;
  }
  const rect = row.getBoundingClientRect();
  row.classList.add(event.clientY > rect.top + rect.height / 2 ? "drop-after" : "drop-before");
});

list.addEventListener("drop", (event) => {
  const row = event.target.closest(".link");
  if (!row || state.dragId == null) {
    return;
  }
  event.preventDefault();
  const dragId = state.dragId;
  const after = row.classList.contains("drop-after");
  state.dragId = null;
  reorderDrop(dragId, row.dataset.id, after);
});

list.addEventListener("dragend", () => {
  state.dragId = null;
  list.querySelectorAll(".link").forEach((item) => {
    item.classList.remove("is-dragging", "drop-before", "drop-after");
  });
});

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const url = urlInput.value.trim();
  const label = labelInput.value.trim();
  if (!isHttpUrl(url)) {
    showError("Enter an absolute http or https URL.");
    urlInput.focus();
    return;
  }
  setFormEnabled(false);
  enqueue(async () => {
    try {
      const response = await fetch("/api/links", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ url, label: label || null })
      });
      if (!response.ok) {
        showError(await errorMessage(response));
        return;
      }
      const created = await response.json();
      state.links.push(created);
      state.selectedId = created.id;
      state.editingId = null;
      state.confirmingId = null;
      rememberSelection();
      urlInput.value = "";
      labelInput.value = "";
      clearError();
      render("#link-" + created.id + " .select");
      document.getElementById("link-" + created.id)?.scrollIntoView({ block: "nearest" });
    } catch {
      showError("Could not add that link.");
    } finally {
      setFormEnabled(true);
    }
  });
});

document.querySelector("#reload-frame").addEventListener("click", () => {
  const frame = document.querySelector("#viewport");
  if (frame.dataset.url && isHttpUrl(frame.dataset.url)) {
    frame.src = frame.dataset.url;
  }
});

matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
  const frame = document.querySelector("#viewport");
  if (!frame.dataset.url) return;
  frame.src = frame.dataset.url;
});

async function start() {
  state.selectedId = storedSelection();
  try {
    await reload();
  } catch {
    showError("Could not load links.");
  } finally {
    setFormEnabled(true);
  }
}

start();
