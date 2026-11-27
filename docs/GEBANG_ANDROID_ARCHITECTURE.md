# Gebang Editor Android Port — Architecture Baseline

This document records a conservative Android implementation boundary based on the inspection of the packaged v0.1.27 application. It does not claim source-level compatibility with Gebang's private project format.

## Layers

1. **Presentation** — recreate the original editor's landscape workspace: top command bar, media/library panel, preview, timeline and inspector. Keep the original information architecture; adapt density and touch targets for phones.
2. **Project domain** — tracks, clips, timing, media references, effects, captions and project lifecycle. The initial Kotlin models are provisional domain types only.
3. **Engine contract** — asynchronous editing commands, project persistence, export progress and cancellation. UI must not call codecs or AI inference directly.
4. **Media adapters** — Android-compatible decoding, frame extraction, audio processing, thumbnails, waveform and rendering. Evaluate Media3 and FFmpeg per required operation and codec.
5. **AI adapters** — ONNX Runtime Android or other ARM64-compatible runtime, with exact model preprocessing/postprocessing verified against the original.
6. **Storage** — Storage Access Framework URIs, permission persistence, missing-media detection, relinking, autosave and recovery.
7. **Entitlement/update integration** — retain the original licensing and premium entitlement behavior; integrate only with creator-approved endpoints/SDKs.

## Initial Kotlin boundary

- `GebangProject.kt`: provisional project-domain model.
- `EditorEngine.kt`: suspend/Flow contract for editing, persistence and export.
- These files are scaffolding, not a working renderer, project codec, export engine or Gebang-compatible implementation.

## Non-negotiable compatibility gates

- Obtain creator-authorized source code or a documented SDK/API.
- Obtain the project file format specification and sample projects.
- Confirm rights to use UI assets, fonts, effects, templates, stickers and AI models.
- Document every Electron IPC method and map it to an Android service.
- Validate original project round-trip (open → edit → save → reopen) before declaring project compatibility.
- Compare rendered output against reference exports for timing, frame rate, audio sync, transitions and effects.
- Keep license activation, premium checks and update behavior intact.

## Milestones

- **M0 — source and rights:** source archive/repository, authorization, asset/model terms, project format.
- **M1 — workspace shell:** faithful landscape UI with real panels and navigation; no fake editing controls.
- **M2 — project/media foundation:** import, relink, project persistence, autosave/recovery.
- **M3 — timeline engine:** multitrack timeline, clip operations, snapping, keyframes and undo/redo.
- **M4 — preview and export:** frame-accurate preview, audio mix, render queue and cancellation.
- **M5 — effects and AI:** transitions/effects, captions/ASR, depth/matting and model lifecycle.
- **M6 — compatibility QA:** project round-trip, output comparison, performance, memory and device matrix.

## Known blockers

- The packaged main process is bytecode, not maintainable source.
- Windows native modules cannot be loaded by Android ARM64.
- Renderer identifiers alone do not prove feature behavior or licensing availability.
- The original project format has not been decoded or validated.
