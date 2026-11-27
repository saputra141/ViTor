class WebKdenliveCore {
  constructor() {
    this.state = {
      clips: [],
      playhead: 0,
      duration: 0,
      isPlaying: false,
      fps: 30,
      layers: [1, 2, 3],
      resolution: { width: 1920, height: 1080 }
    };
    this.canvas = document.createElement('canvas');
    this.ctx = this.canvas.getContext('2d', { alpha: false });
    this.offscreenVideos = new Map();
    this.lastFrameTime = 0;
    this.init();
  }

  init() {
    const previewContainer = document.querySelector('.video-preview');
    if (previewContainer) {
      this.canvas.className = 'preview-canvas';
      this.canvas.width = 1280;
      this.canvas.height = 720;
      previewContainer.appendChild(this.canvas);
    }
    this.setupEventListeners();
    this.renderLoop();
  }

  setupEventListeners() {
    const importBtn = document.createElement('input');
    importBtn.type = 'file';
    importBtn.multiple = true;
    importBtn.accept = 'video/*,image/*';
    importBtn.id = 'hidden-file-input';
    importBtn.style.display = 'none';
    document.body.appendChild(importBtn);

    document.addEventListener('click', (e) => {
      if (e.target.innerText === 'Project' || e.target.classList.contains('nav-btn')) {
        importBtn.click();
      }
    });

    importBtn.addEventListener('change', (e) => this.handleMediaImport(e));

    window.addEventListener('keydown', (e) => {
      if (e.code === 'Space') {
        e.preventDefault();
        this.togglePlayback();
      }
    });
  }

  async handleMediaImport(event) {
    const files = Array.from(event.target.files);
    for (const file of files) {
      const clip = await this.createClip(file);
      this.state.clips.push(clip);
      this.updateProjectDuration();
      console.log('Clip added:', clip.name);
    }
  }

  async createClip(file) {
    const id = Math.random().toString(36).substr(2, 9);
    const url = URL.createObjectURL(file);
    const type = file.type.startsWith('video') ? 'video' : 'image';
    let duration = 5;

    if (type === 'video') {
      duration = await this.getVideoDuration(url);
      const video = document.createElement('video');
      video.src = url;
      video.muted = true;
      video.preload = 'auto';
      this.offscreenVideos.set(id, video);
    }

    return {
      id,
      name: file.name,
      type,
      url,
      start: 0,
      duration,
      layer: 1,
      offset: 0
    };
  }

  getVideoDuration(url) {
    return new Promise((resolve) => {
      const video = document.createElement('video');
      video.src = url;
      video.onloadedmetadata = () => resolve(video.duration);
    });
  }

  updateProjectDuration() {
    this.state.duration = Math.max(...this.state.clips.map(c => c.start + c.duration), 0);
  }

  togglePlayback() {
    this.state.isPlaying = !this.state.isPlaying;
    this.lastFrameTime = performance.now();
  }

  renderLoop() {
    const now = performance.now();
    if (this.state.isPlaying) {
      const delta = (now - this.lastFrameTime) / 1000;
      this.state.playhead += delta;
      if (this.state.playhead >= this.state.duration) {
        this.state.playhead = 0;
      }
      this.lastFrameTime = now;
    }

    this.drawFrame();
    requestAnimationFrame(() => this.renderLoop());
  }

  drawFrame() {
    this.ctx.fillStyle = '#000';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    const activeClips = this.state.clips
      .filter(clip => this.state.playhead >= clip.start && this.state.playhead <= (clip.start + clip.duration))
      .sort((a, b) => a.layer - b.layer);

    activeClips.forEach(clip => {
      if (clip.type === 'video') {
        const video = this.offscreenVideos.get(clip.id);
        if (video) {
          const videoTime = this.state.playhead - clip.start + clip.offset;
          if (Math.abs(video.currentTime - videoTime) > 0.1) {
            video.currentTime = videoTime;
          }
          this.ctx.drawImage(video, 0, 0, this.canvas.width, this.canvas.height);
        }
      } else if (clip.type === 'image') {
        const img = new Image();
        img.src = clip.url;
        this.ctx.drawImage(img, 0, 0, this.canvas.width, this.canvas.height);
      }
    });
  }
}

window.addEventListener('DOMContentLoaded', () => {
  window.app = new WebKdenliveCore();
});