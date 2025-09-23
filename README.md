# CCA (Character Chat App)
A privacy-focused, open-source AI chat interface designed for local, private role-play and character interaction. This application leverages your local hardware to run large language models (LLMs) for immersive character experiences.

Unlike many existing solutions, this project prioritizes a fully offline and local experience. No subscriptions, no purchases, and it's completely open source.

**Important:** You'll need a mid-range gaming GPU with 8GB VRAM or more!

<img alt="Home" height="700" src="images/Home.png" width="1200"/>

## ✨ Features
* **Role-Play Focused:** Optimized for engaging and immersive character interactions.
* **Character Management:** Create and manage character templates, supporting imports from popular character card formats (SillyTavern, Backyard AI).
* **User Templates:** Define and customize multiple user profiles for diverse interactions.
* **Broad GPU Support:** Designed to utilize various GPUs via accelerated frameworks (CUDA, HIP, Vulkan.).
* **Open Source:** Full transparency with an open codebase for inspection and modification.
* **Privacy-Centric:**
    * **No Data Collection:** Your data stays on your machine.
    * **Encrypted Data:** Sensitive information (chats, characters, users) is encrypted locally.
* **Advanced Model Features:** Support for Qwen3 Think Mode and Jinja templating.
* **Reasoning Support:** Think tags are separated into its own collapsable view.

## 💻 Compatibility
Only works with Windows 10/11. Linux and Mac are being explored.

### Mobile
Some views are mobile supported but the application isn't. You will have to use remote desktop applications to connect to your pc remotely.

The views are extremely experimental and may not function properly.

## Code Stack
* [JavaFX](https://openjfx.io) The GUI library.
* [AtlantaFX](https://github.com/mkpaz/atlantafx) Modernized styling for JavaFX.
* [RenEngine](https://github.com/HackusatePvP/RenEngine) Simplified framework for JavaFX.
* [llama.cpp](https://github.com/ggml-org/llama.cpp) Backend server for the models.

## Multimodal / Vision
As of version 1.0.4 vision has been implemented into the chats. When sending an image it will now add the image to the chat window. You can close and expand the image when needed. The model can only process one image, it will prioritize the last sent message.

## Vulkan
Vulkan works right out of the box. No installations needed apart from basic graphics drivers. As of version 1.0.5 vulkan is the default backend.

Q: What's the difference between the backends?

A: Cuda and HIP typically provide better performance, but you will have to install necessary drivers. Vulkan works for most GPUs without the need of third party installations. Cuda requires an Nvidia card. HIP requires an AMD card.

## 🚀 Installation

This section outlines the general steps to get you started. More detailed instructions for specific dependencies are provided in the sections that follow.

1. Download [Releases.zip](https://github.com/HackusatePvP/character-chat-app/releases).
2. (Optional) Download the updated jar if applicable.
3. (Optional) Download AI GPU drivers (Cuda, HIP)
4. Extract all files into `%APPDATA%/chat-app`
5. Enter `chat-app`
6. Run `run.bat`

## Install Application
Download the official release from [Releases](https://github.com/HackusatePvP/character-chat-app/releases)

Extract the zip file into %APPDATA%. In your Roaming folder you should see a new folder called `chat-app`

Next run the application.

1. You might be able to launch the .jar file by clicking on it like an exe. (Only works if you have java configured properly)
2. Execute `run.bat` or use the java command `java -jar {jar-name}.jar`

## Installing / Downloading Models
Only GGUF format is supported. Please refer to [Converting Models]() for converting to GGUF models.

Configure the model location in the `Models` tab on the application. It is highly recommended to change it, but you can keep it default.

If you place a model inside the directory you will have to refresh the page. (Click on `Models` tab again.)

Make sure you have a model set as a default. This is a hard requirement.

## Compile Source
You will have to have RenEngine compiled.

In the project directory run the follow.
```bash
mvnw clean install
```
There will be two jar files.
1. character-chat-app.jar : This is the executable file.
2. character-chat-app-1.0-SNAPSHOT.jar : Compiled classes only.

Download the latest releases. This is needed for necessary backend and sdk files. Replace the jarfile with your compiled executable jar file.
