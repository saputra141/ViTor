# WebKdenlive-Lite

WebKdenlive-Lite is a high-performance, browser-based video and photo editing application inspired by the power and versatility of the KDE Kdenlive project. It provides a professional-grade editing experience directly in the web browser, utilizing modern web technologies like WebAssembly, WebGL, and FFmpeg.wasm.

## Key Features

- **Non-Linear Video Editing**: A robust multi-track timeline allowing for complex arrangements of video clips, audio tracks, and images.
- **Canvas-Based Filters**: High-performance image and video processing using WebGL shaders and CSS filters for real-time visual feedback.
- **Universal Media Support**: Capability to handle various formats directly in the browser via FFmpeg.wasm integration.
- **Professional Photo Suite**: Integrated photo editing features including layers, blending modes, and non-destructive adjustments.
- **Browser-Side Rendering**: Fast and private export process that occurs entirely on the client machine.

## Prerequisites

Before you begin, ensure you have the following installed:
- **Node.js**: Version 18.x or higher
- **npm** or **yarn**: Modern package manager
- **Modern Browser**: Chrome, Edge, or Firefox (Browsers must support `SharedArrayBuffer` for multi-threaded FFmpeg performance)

## Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/webkdenlive-lite.git
   cd webkdenlive-lite
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start the development server:**
   ```bash
   npm run dev
   ```

4. **Access the application:**
   Open [http://localhost:5173](http://localhost:5173) in your web browser.

## How to Use

### Adding Media to the Timeline

1. **Import Assets**: Click the 'Import Media' button in the **Project Bin** (top-left) or simply drag and drop your files from your computer into the bin area.
2. **Place on Timeline**: Click and hold a clip in the Project Bin, then drag it down onto a **Video Track (V1, V2)** or **Audio Track (A1, A2)** in the timeline.
3. **Manipulation**: Once on the timeline, you can:
   - **Move**: Drag the clip left or right to adjust the start time.
   - **Trim**: Drag the edges of the clip to adjust its duration.
   - **Split**: Position the playhead and use the 'Razor' tool (C) to cut clips.

### Applying Filters

1. Select a clip in the timeline so it is highlighted.
2. Go to the **Effects/Filters** tab in the properties panel.
3. Choose a filter (e.g., Color Correction, Gaussian Blur, or Transform).
4. Adjust the sliders; the changes will reflect immediately in the **Video Preview** canvas.

## Technical Stack

- **Frontend**: React / TypeScript
- **Rendering**: HTML5 Canvas / WebGL
- **Processing**: FFmpeg.wasm
- **State Management**: Redux Toolkit (Timeline State)

## License

This project is licensed under the GPL-3.0 License - consistent with the Kdenlive open-source philosophy.