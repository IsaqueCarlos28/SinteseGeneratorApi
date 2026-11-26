(() => {
  const TEXT_ENDPOINT = "/api/sinteses";
  const FILE_ENDPOINT = "/api/sinteses/documentos";
  const MAX_WORDS = 50000;
  const MAX_FILE_SIZE = 10 * 1024 * 1024;
  const MAX_REQUEST_SIZE = 30 * 1024 * 1024;
  // Margem conservadora para boundaries/cabeçalhos multipart; o backend aplica 30 MB na requisição total.
  const MULTIPART_OVERHEAD_MARGIN = 256 * 1024;
  const MAX_FILES_TOTAL_SIZE = MAX_REQUEST_SIZE - MULTIPART_OVERHEAD_MARGIN;
  const ALLOWED_EXTENSIONS = [".txt", ".pdf", ".docx"];

  const tabs = Array.from(document.querySelectorAll(".tab"));
  const panels = {
    texto: document.getElementById("panel-texto"),
    arquivos: document.getElementById("panel-arquivos"),
  };

  const textDocumentsEl = document.getElementById("text-documents");
  const addDocumentBtn = document.getElementById("add-document");
  const textWordCountEl = document.getElementById("text-word-count");

  const fileInput = document.getElementById("file-input");
  const fileListEl = document.getElementById("file-list");

  const submitBtn = document.getElementById("submit-btn");
  const clearBtn = document.getElementById("clear-btn");
  const copyBtn = document.getElementById("copy-btn");
  const statusEl = document.getElementById("status");
  const resultBox = document.getElementById("result-box");
  const errorBox = document.getElementById("error-box");

  let activeTab = "texto";
  let selectedFiles = [];
  let lastResult = "";

  function countWords(text) {
    const trimmed = text.trim();
    if (!trimmed) return 0;
    return trimmed.split(/\s+/).length;
  }

  function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    const kb = bytes / 1024;
    if (kb < 1024) return `${kb.toFixed(1)} KB`;
    return `${(kb / 1024).toFixed(2)} MB`;
  }

  function getExtension(name) {
    const idx = name.lastIndexOf(".");
    return idx >= 0 ? name.slice(idx).toLowerCase() : "";
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function setStatus(message, type = "") {
    statusEl.className = `status ${type}`.trim();
    if (type === "loading") {
      statusEl.innerHTML = `<span class="spinner" aria-hidden="true"></span><span>${escapeHtml(message)}</span>`;
      return;
    }
    statusEl.textContent = message;
  }

  function clearError() {
    errorBox.hidden = true;
    errorBox.innerHTML = "";
  }

  function showError(title, detail, meta = []) {
    const metaHtml = meta
      .filter(Boolean)
      .map((item) => `<p class="error-meta">${escapeHtml(item)}</p>`)
      .join("");

    errorBox.hidden = false;
    errorBox.innerHTML = `
      <h3>${escapeHtml(title || "Erro")}</h3>
      <p>${escapeHtml(detail || "Ocorreu um erro ao processar a requisição.")}</p>
      ${metaHtml}
    `;
  }

  function setResult(text) {
    lastResult = text || "";
    if (lastResult) {
      resultBox.innerHTML = `<p class="result-text">${escapeHtml(lastResult)}</p>`;
      copyBtn.disabled = false;
    } else {
      resultBox.innerHTML = `<p class="empty-state">A síntese aparecerá aqui após o envio.</p>`;
      copyBtn.disabled = true;
    }
  }

  function updateWordCount() {
    const areas = textDocumentsEl.querySelectorAll("textarea");
    let total = 0;
    areas.forEach((area) => {
      const words = countWords(area.value);
      total += words;
      const meta = area.closest(".document-card")?.querySelector(".document-meta");
      if (meta) meta.textContent = `${words} palavra${words === 1 ? "" : "s"}`;
    });
    textWordCountEl.textContent = `Total estimado: ${total} palavra${total === 1 ? "" : "s"}`;
    textWordCountEl.classList.toggle("warn", total > MAX_WORDS);
  }

  function createDocumentCard(value = "") {
    const index = textDocumentsEl.children.length + 1;
    const card = document.createElement("article");
    card.className = "document-card";

    const textareaId = `documento-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    card.innerHTML = `
      <div class="document-head">
        <label for="${textareaId}">Documento ${index}</label>
        <button type="button" class="btn btn-danger remove-document">Remover</button>
      </div>
      <textarea id="${textareaId}" placeholder="Cole ou digite o conteúdo do documento aqui..."></textarea>
      <div class="document-meta">0 palavras</div>
    `;

    const textarea = card.querySelector("textarea");
    textarea.value = value;
    textarea.addEventListener("input", updateWordCount);

    card.querySelector(".remove-document").addEventListener("click", () => {
      if (textDocumentsEl.children.length === 1) {
        textarea.value = "";
        updateWordCount();
        textarea.focus();
        return;
      }
      card.remove();
      renumberDocuments();
      updateWordCount();
    });

    textDocumentsEl.appendChild(card);
    renumberDocuments();
    updateWordCount();
    return textarea;
  }

  function renumberDocuments() {
    Array.from(textDocumentsEl.children).forEach((card, idx) => {
      const label = card.querySelector("label");
      if (label) label.textContent = `Documento ${idx + 1}`;
    });
  }

  function renderFileList() {
    if (selectedFiles.length === 0) {
      fileListEl.innerHTML = `<p class="empty-state">Nenhum arquivo selecionado.</p>`;
      return;
    }

    const totalSize = selectedFiles.reduce((sum, file) => sum + file.size, 0);
    const items = selectedFiles
      .map((file, index) => {
        const ext = getExtension(file.name);
        const extWarning = !ALLOWED_EXTENSIONS.includes(ext)
          ? '<span class="file-size"> · Extensão não suportada</span>'
          : "";
        const sizeWarning = file.size > MAX_FILE_SIZE
          ? '<span class="file-size"> · Acima de 10 MB</span>'
          : "";
        return `
          <div class="file-item">
            <div class="file-info">
              <span class="file-name">${escapeHtml(file.name)}</span>
              <span class="file-size">${formatBytes(file.size)}${extWarning}${sizeWarning}</span>
            </div>
            <button type="button" class="btn btn-danger" data-remove-file="${index}">Remover</button>
          </div>
        `;
      })
      .join("");

    const requestWarning = totalSize > MAX_FILES_TOTAL_SIZE
      ? `<p class="file-size" style="color:#b91c1c;margin:0.25rem 0 0;">Tamanho total dos arquivos: ${formatBytes(totalSize)} (acima do limite seguro de envio; a requisição suporta até 30 MB no total, incluindo overhead multipart)</p>`
      : `<p class="file-size" style="margin:0.25rem 0 0;">Tamanho total dos arquivos: ${formatBytes(totalSize)}</p>`;

    fileListEl.innerHTML = `${items}${requestWarning}`;

    fileListEl.querySelectorAll("[data-remove-file]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const idx = Number(btn.getAttribute("data-remove-file"));
        selectedFiles.splice(idx, 1);
        syncFileInput();
        renderFileList();
      });
    });
  }

  function syncFileInput() {
    const dt = new DataTransfer();
    selectedFiles.forEach((file) => dt.items.add(file));
    fileInput.files = dt.files;
  }

  function addFiles(fileList) {
    const incoming = Array.from(fileList || []);
    incoming.forEach((file) => {
      const exists = selectedFiles.some(
        (f) => f.name === file.name && f.size === file.size && f.lastModified === file.lastModified
      );
      if (!exists) selectedFiles.push(file);
    });
    syncFileInput();
    renderFileList();
  }

  function setActiveTab(tabName) {
    activeTab = tabName;
    tabs.forEach((tab) => {
      const isActive = tab.dataset.tab === tabName;
      tab.classList.toggle("active", isActive);
      tab.setAttribute("aria-selected", String(isActive));
      tab.tabIndex = isActive ? 0 : -1;
    });

    Object.entries(panels).forEach(([name, panel]) => {
      const isActive = name === tabName;
      panel.classList.toggle("active", isActive);
      panel.hidden = !isActive;
    });

    clearError();
    setStatus("");
  }

  function validateTextMode() {
    const values = Array.from(textDocumentsEl.querySelectorAll("textarea")).map((t) => t.value);
    if (values.length === 0) {
      return { ok: false, message: "Adicione pelo menos um documento." };
    }
    if (values.some((v) => !v.trim())) {
      return { ok: false, message: "Preencha todos os documentos. Conteúdo vazio não é permitido." };
    }
    const totalWords = values.reduce((sum, v) => sum + countWords(v), 0);
    if (totalWords > MAX_WORDS) {
      return {
        ok: false,
        message: `A soma das palavras (${totalWords}) ultrapassa o limite de ${MAX_WORDS}.`,
      };
    }
    return { ok: true, values };
  }

  function validateFileMode() {
    if (selectedFiles.length === 0) {
      return { ok: false, message: "Selecione pelo menos um arquivo." };
    }

    for (const file of selectedFiles) {
      const ext = getExtension(file.name);
      if (!ALLOWED_EXTENSIONS.includes(ext)) {
        return {
          ok: false,
          message: `O arquivo "${file.name}" tem extensão não suportada. Use .txt, .pdf ou .docx.`,
        };
      }
      if (file.size === 0) {
        return { ok: false, message: `O arquivo "${file.name}" está vazio.` };
      }
      if (file.size > MAX_FILE_SIZE) {
        return { ok: false, message: `O arquivo "${file.name}" excede o limite de 10 MB.` };
      }
    }

    const totalSize = selectedFiles.reduce((sum, f) => sum + f.size, 0);
    if (totalSize > MAX_FILES_TOTAL_SIZE) {
      return {
        ok: false,
        message: "A soma dos arquivos excede o limite seguro de envio. A requisição suporta até 30 MB no total, incluindo overhead do envio multipart.",
      };
    }

    return { ok: true };
  }

  async function parseResponse(response) {
    const contentType = (response.headers.get("content-type") || "").toLowerCase();
    if (contentType.includes("json")) {
      try {
        return await response.json();
      } catch {
        return null;
      }
    }
    return null;
  }

  function getRetryAfterSeconds(response) {
    const value = response.headers.get("Retry-After");
    if (!value) return null;
    const seconds = Number.parseInt(value, 10);
    return Number.isFinite(seconds) && seconds > 0 ? seconds : null;
  }

  function resolveErrorMessage(response, payload) {
    const status = response.status;
    const hasJson = payload && typeof payload === "object";
    const apiTitle = typeof payload?.title === "string" ? payload.title : "";
    const apiDetail = typeof payload?.detail === "string" ? payload.detail : "";

    if (status === 400) {
      return {
        title: "Requisição inválida",
        detail: apiDetail || "Verifique os dados enviados e tente novamente.",
      };
    }
    if (status === 413) {
      return {
        title: "Arquivo muito grande",
        detail: "Cada arquivo pode ter até 10 MB e a requisição até 30 MB.",
      };
    }
    if (status === 415 || status === 422) {
      return {
        title: apiTitle || (status === 415 ? "Tipo de conteúdo não suportado" : "Documento não processável"),
        detail: apiDetail || "Não foi possível processar o conteúdo enviado.",
      };
    }
    if (status === 429) {
      return {
        title: "Muitas requisições",
        detail: "O limite de uso da IA foi atingido. Aguarde um pouco e tente novamente.",
      };
    }
    if (status === 502) {
      if (!hasJson) {
        return {
          title: "Serviço indisponível",
          detail: "O servidor pode estar iniciando. Aguarde alguns segundos e tente novamente.",
        };
      }
      return {
        title: "Falha ao gerar a síntese",
        detail: "Não foi possível processar a síntese agora. Tente novamente.",
      };
    }
    if (status === 503) {
      if (!hasJson) {
        return {
          title: "Serviço indisponível",
          detail: "O servidor pode estar iniciando. Aguarde alguns segundos e tente novamente.",
        };
      }
      const retryAfter = getRetryAfterSeconds(response);
      const waitMessage = retryAfter
        ? `Aguarde cerca de ${retryAfter} segundos e tente novamente.`
        : "Aguarde alguns instantes e tente novamente.";
      return {
        title: "IA com alta demanda",
        detail: waitMessage,
      };
    }
    if (status === 504) {
      if (!hasJson) {
        return {
          title: "Serviço indisponível",
          detail: "O servidor pode estar iniciando. Aguarde alguns segundos e tente novamente.",
        };
      }
      return {
        title: "A IA demorou demais",
        detail: "Tente novamente ou envie um conteúdo menor.",
      };
    }
    if (status === 500 || !hasJson) {
      return {
        title: "Erro inesperado",
        detail: "Algo deu errado. Tente novamente em instantes.",
      };
    }

    return {
      title: apiTitle || "Erro inesperado",
      detail: apiDetail || "Algo deu errado. Tente novamente em instantes.",
    };
  }

  function buildStatusCodeMeta(status) {
    return Number.isFinite(status) ? [`Código ${status}`] : [];
  }

  async function submit() {
    clearError();
    setStatus("");

    let url;
    let options;

    if (activeTab === "texto") {
      const validation = validateTextMode();
      if (!validation.ok) {
        showError("Requisição inválida", validation.message);
        return;
      }
      url = TEXT_ENDPOINT;
      options = {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          documentos: validation.values.map((conteudo) => ({ conteudo })),
        }),
      };
    } else {
      const validation = validateFileMode();
      if (!validation.ok) {
        showError("Arquivo inválido", validation.message);
        return;
      }
      const formData = new FormData();
      selectedFiles.forEach((file) => formData.append("documentos", file, file.name));
      url = FILE_ENDPOINT;
      options = { method: "POST", body: formData };
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 60000);
    const slowStatusId = setTimeout(() => {
      if (submitBtn.disabled) {
        setStatus("Ainda processando... isso pode levar alguns segundos.", "loading");
      }
    }, 5000);

    submitBtn.disabled = true;
    submitBtn.textContent = "Gerando síntese...";
    setStatus("Enviando conteúdo e aguardando a resposta da API...", "loading");

    try {
      const response = await fetch(url, { ...options, signal: controller.signal });
      const payload = await parseResponse(response);

      if (!response.ok) {
        const error = resolveErrorMessage(response, payload);
        showError(error.title, error.detail, buildStatusCodeMeta(response.status));
        setStatus("A requisição não foi concluída.", "");
        return;
      }

      const sintese = payload?.sintese;
      if (typeof sintese !== "string" || !sintese.trim()) {
        showError(
          "Resposta inesperada",
          "A API respondeu com sucesso, mas o campo 'sintese' não foi encontrado.",
          buildStatusCodeMeta(response.status)
        );
        setStatus("Resposta inesperada da API.", "");
        return;
      }

      setResult(sintese);
      setStatus("Síntese gerada com sucesso.", "success");
    } catch (err) {
      if (err?.name === "AbortError") {
        showError("Tempo limite excedido", "O processamento ultrapassou o tempo esperado. Tente novamente.");
        setStatus("A requisição não foi concluída.", "");
      } else {
        showError(
          "Falha de conexão",
          "Não foi possível conectar à API. Verifique se a aplicação está rodando e tente novamente."
        );
        setStatus("Falha de conexão com a API.", "");
      }
    } finally {
      clearTimeout(timeoutId);
      clearTimeout(slowStatusId);
      submitBtn.disabled = false;
      submitBtn.textContent = "Gerar síntese";
    }
  }

  async function copyResult() {
    if (!lastResult) return;
    try {
      await navigator.clipboard.writeText(lastResult);
      setStatus("Síntese copiada para a área de transferência.", "success");
    } catch {
      const textarea = document.createElement("textarea");
      textarea.value = lastResult;
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand("copy");
        setStatus("Síntese copiada para a área de transferência.", "success");
      } catch {
        setStatus("Não foi possível copiar automaticamente.", "");
      } finally {
        document.body.removeChild(textarea);
      }
    }
  }

  function clearAll() {
    clearError();
    setStatus("");
    setResult("");

    textDocumentsEl.innerHTML = "";
    createDocumentCard();

    selectedFiles = [];
    syncFileInput();
    renderFileList();
    fileInput.value = "";
  }

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => setActiveTab(tab.dataset.tab));
    tab.addEventListener("keydown", (e) => {
      if (e.key !== "ArrowRight" && e.key !== "ArrowLeft") return;
      e.preventDefault();
      const currentIdx = tabs.indexOf(tab);
      const dir = e.key === "ArrowRight" ? 1 : -1;
      const next = tabs[(currentIdx + dir + tabs.length) % tabs.length];
      next.focus();
      setActiveTab(next.dataset.tab);
    });
  });

  addDocumentBtn.addEventListener("click", () => {
    const textarea = createDocumentCard();
    textarea.focus();
  });

  fileInput.addEventListener("change", (e) => {
    addFiles(e.target.files);
    fileInput.value = "";
  });

  submitBtn.addEventListener("click", submit);
  clearBtn.addEventListener("click", clearAll);
  copyBtn.addEventListener("click", copyResult);

  createDocumentCard();
  renderFileList();
  setActiveTab("texto");
})();
