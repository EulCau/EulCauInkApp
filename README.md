# EulCauInk

EulCauInk is a **local-first note-taking application** centered around **Markdown (`.md`)**, with a strong focus on **structured writing, lightweight editing, and full offline availability**.  
The app is built using Web technologies and packaged as a native Android application via Android WebView.

## Features

- Local Markdown note management
- Instant editing with a WYSIWYG-like Markdown experience
- Drag-and-drop note reordering
- Note deletion support
- Drawing and image insertion within Markdown
- Support for multiple hyperlink types
- Native Android app packaging with offline support

## Update Plan

- [x] Upload and export Markdown files (v1.0.1)
- [x] Fix image rendering issues (v1.0.1)
- [x] Upload images within notes (v1.0.1)
- [x] Save images rended in notes (v1.0.1)
- [ ] Improve checkbox display
- [ ] Improve code block rendering
- [ ] Ctrl + S to save
- [ ] Silent save (display `*` in the title when there are unsaved changes)
- [ ] More drawing features
- [ ] Code hints and auto-completion
- [ ] AI integration

## App Usage Guide

### 1️. Creating and Editing Notes

- After launching the app, you can **create new Markdown (`.md`) files**
- Tap a note to enter the editor
- All notes are stored locally and do not rely on cloud services

### 2️. Note Management

- **Drag-and-drop reordering**  
  Long-press a note to adjust its position
- **Delete notes**  
  Unneeded `.md` files can be removed directly

### 3️. Markdown Capabilities

Within a Markdown document, you can:

- Write standard Markdown syntax
- Upload / download Markdown files
- Draw or insert images
- Insert hyperlinks

#### Supported Link Types

| Link Type | Supported | Description |
| --------- | --------- | ----------- |
| `http://` / `https://` | ✅ | External web links |
| `#heading` | ✅ | Jump to headings within the current document |
| `xxx.md` | ✅ | Navigate to another Markdown document |
| `mailto:` | ✅ | Email links |
| `tel:` | ✅ | Telephone links |
| `file://` | ❌ | Not supported due to security restrictions |

> **Note**:  
> Due to Android WebView security constraints, the `file://` protocol is not available. Please use one of the supported link formats above.

## Technical Architecture Overview

- **Frontend**: Vite + React + Tailwind CSS
- **Markdown Rendering**: `react-markdown` with `remark` / `rehype`
- **Platform**: Android WebView
- **Build Pipeline**: Vite build + local static asset loading

## Web Source Code

The complete web frontend source code is available at:

**GitHub Repository**: [EulCau/EulCauInk](https://github.com/EulCau/EulCauInk)

Prebuilt static assets can be downloaded from the  
[Releases page](https://github.com/EulCau/EulcauInk/releases)

This repository includes:

- Frontend source code
- Vite build configuration
- Markdown editing and rendering logic
- Static site build and packaging instructions

## Known Limitations and Notes

- This application is **not intended for deployment as a public web service**
- In the WebView environment:
  - CDN resources must be accessible
  - `file://` links are restricted
- WebView behavior in release mode is stricter than in debug mode; pay close attention to resource paths and permissions

## License

This project is licensed under the MIT License.

## Acknowledgements

- Open-source communities including React, Tailwind CSS, CodeMirror, and Android WebView
- AI-assisted development with ChatGPT and AI Studio
