# Gebang Editor Android Port — Initial Technical Inventory

Date: 2026-11-27
Source inspected: official Gebang Editor v0.1.27 packaged application, extracted from its Windows portable release. This is an inventory of observed package interfaces and strings, not proof that each feature has been runtime-tested.

## Renderer and application structure
- Renderer entry: `out/renderer/index.html`
- Main renderer bundle: `out/renderer/assets/index-D7Te3vRf.js`
- Renderer is a bundled JavaScript application with a root mount element.
- Preload bridge: `out/preload/index.js`
- Main-process entry metadata points to `out/main/index.cjs`; the main bundle is packaged as bytecode (`.cjsc`) and is not readable source in the extracted inspection.
- Electron/Node APIs and Windows native modules are not directly portable to Android ARM64.

## Feature surface detected in renderer
- Editing foundation: projects, timeline, tracks, clips, keyframes, transitions, speed, trim, crop, proxy, waveform.
- Visual tools: effects, filters, masks, overlays, stickers, chroma.
- Media: import, video, audio, media probing, thumbnails, scene/motion analysis.
- Captions and speech: caption/transcription, speech/ASR, lyrics, Quran and sholawat-related interfaces.
- AI/media models: depth, ONNX, Whisper-related identifiers, model download/status management.
- Export/render: export jobs, progress, frame transfer, cancellation, audio chunk transfer.
- Project lifecycle: new/open/save/save-as/autosave, recents, recovery, missing-media checks and relinking.
- Additional interfaces: recorder, fonts, effect catalog, sticker catalog, DJ templates, media libraries, LUT handling, diagnostics and settings.

## Preload bridge groups observed
The `api` object exposes IPC methods for:
- License activation/status/deactivation/refresh.
- Update status/download/install.
- Export destination selection, output info, export start/cancel/progress/done/error and frame/audio transfer.
- Recorder list/permissions/start/commands/status.
- Media dialog, path expansion, file probe, packet index, thumbnail, waveform, asset registration, media derivation, envelope and LUT operations.
- Effects and fonts listing/import/removal.
- Project lifecycle, autosave and recovery.
- Settings/cache, depth status, media libraries and global media.
- DJ template import/export/store.
- Matte/depth frame writing and tracking jobs.
- AI model management, caption audio/tempo/transcription and Quran/sholawat data.

## Android port implications
1. Keep the original interaction model and visual hierarchy as the reference; adapt layout for landscape phone screens without reducing it to a generic list editor.
2. Replace Electron IPC/preload with a documented Android bridge/native service layer.
3. Replace Windows-only native modules with Android ARM64-compatible equivalents. Evaluate Media3/FFmpeg and ONNX Runtime Android per feature; do not assume one replacement covers every codec/effect.
4. Treat project file compatibility, autosave/recovery, media relinking, export progress and cancellation as core requirements.
5. Reimplement or port AI pipelines only after identifying exact models, preprocessing, postprocessing and licensing requirements.
6. Preserve the original license and entitlement behavior. Do not remove activation, bypass premium checks, or strip license enforcement.
7. Validate feature-by-feature on physical Android hardware; do not claim a feature is complete based on symbol detection alone.

## Important limitations and open questions
- The packaged main process is bytecode, so its internal implementation and IPC handlers cannot be fully reconstructed from this inspection.
- Renderer symbol matches establish that feature-related code/identifiers exist, not that every feature is enabled, complete or available in every license tier.
- The official public release repository provides packaged release assets, not the original maintainable project source.
- A faithful, maintainable Android port will be materially more reliable with the creator's source repository/source archive and written authorization covering code, assets, models and license integration.

## Next required input
Obtain the creator-authorized source project (renderer, main process, build configuration and native/media modules), plus documentation for the project format and model assets. Until then, proceed with a verified interface inventory and Android architecture plan rather than pretending a native port can be completed from the packaged EXE alone.
