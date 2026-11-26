# Speech-to-text and lyric alignment design

## Product modes

1. **Speech captions**: transcribe spoken dialogue from a selected video/audio asset. Produce sentence/phrase cues with timestamps and optional speaker labels.
2. **Song lyrics**: transcribe singing from a selected audio/video asset. Preserve line breaks and support word-level timestamps for karaoke highlighting. Singing recognition is a separate quality target from ordinary speech recognition.

## Local-first architecture

- Media input: Android document picker; retain read permission and work from the selected URI.
- Audio extraction: decode the source audio track to mono PCM at the sample rate required by the selected model. Process long media in bounded chunks to control memory use.
- ASR adapter: a replaceable interface around an on-device engine. Candidate engines are whisper.cpp (multilingual transcription) and sherpa-onnx (offline ASR and alignment capabilities).
- Model manager: model metadata, language, file size, checksum, download/resume, storage location, and deletion. The model is not silently bundled into the APK.
- Alignment: preserve segment timestamps from ASR; for lyrics, add forced/word alignment when a compatible model is available.
- Subtitle domain: store all times as milliseconds in `SubtitleCue`; keep optional `WordTiming` entries for karaoke.
- Editing: allow manual correction of text and timing before export.
- Export: SRT/WebVTT first; later render subtitles into the video through the export pipeline.

## Quality and safety gates

- Never label a transcript as accurate without comparing it against the source audio.
- Do not generate fake timestamps by dividing the total duration evenly across words.
- Show model language and whether processing is offline.
- Keep original audio/video unchanged; subtitle generation is non-destructive.
- Provide cancellation and progress reporting for long transcription jobs.
- Test Indonesian and English speech separately from singing; evaluate noisy vlog audio and music with vocals.

## Current implementation status

The subtitle data model and SRT codec are implemented. ASR, audio extraction, model management, forced alignment, subtitle UI, and burn-in export are not implemented yet.
