# CCA (Character Chat App)
A privacy-focused, open-source AI chat interface designed for local, private role-play and character interaction. This application leverages your local hardware to run large language models (LLMs) for immersive character experiences.

Unlike many existing solutions, this project prioritizes a fully offline and local experience. No subscriptions, no purchases, and it's completely open source.

**Important:** You'll need a mid-range gaming GPU with 8GB VRAM or more!

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

## 💻 Compatibility
Only works with Windows 10/11. Linux and Mac are being explored.

## Code Stack
* [JavaFX](https://openjfx.io) The GUI library.
* [AtlantaFX](https://github.com/mkpaz/atlantafx) Modernized styling for JavaFX.
* [RenEngine](https://github.com/HackusatePvP/RenEngine) Simplified framework for JavaFX.
* [llama.cpp](https://github.com/ggml-org/llama.cpp) Backend server for the models.


## Multimodal / Vision
Vision is now supported as of version 1.0.1. Vision allows a model to process an image as context. Due to consumer hardware limitations only one image can be processed at a time.
This is still being worked on and improved. Currently, there is no display for the image.

## Vulkan
Vulkan has been re-enabled as of version 1.0.1. For Nvidia 50 series and 40 series it can still bluescreen when exiting the application. Please use Cuda if you encounter a crash.

Vulkan works right out of the box. No installations needed apart from basic graphics drivers.

## 🚀 Installation

This section outlines the general steps to get you started. More detailed instructions for specific dependencies are provided in the sections that follow.

1.  **Download Java 21:** Obtain and install the Java 21 Development Kit (JDK).
2.  **Configure Java Path:** Add Java 21 to your system's environment path variables.
3.  **Install Visual Studio (NVIDIA Only):** Download and install Visual Studio with the `Desktop development with C++` workload selected.
4.  **Install GPU Drivers/Frameworks:**
    *   **NVIDIA:** Download and install [CUDA 12.4](https://developer.nvidia.com/cuda-12-4-0-download-archive).
    *   **AMD:** Install the necessary [HIP](https://www.amd.com/en/developer/resources/rocm-hub/hip-sdk.html) drivers (specific instructions may vary by OS).
5.  **Download the Application:** Get the latest release from the [Releases](https://github.com/HackusatePvP/character-chat-app/releases/) page.
6.  **Extract Application:** Extract the downloaded application files into your `%appdata%` directory (or equivalent on other OS).
7.  **Run the Application:** Execute `start.bat` in the folder that was extracted.

## Java Installation
Please download **JDK 21**, the application was tested with [Corretto 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html).

To verify your Java installation, open a command prompt/terminal/shell and type:
```shell
java --version
```
Ensure the output version starts with 21. You should see something like:

`openjdk 21.0.7 2025-04-15 LTS`

If you receive `'java' is not recognized as an internal or external command, operable program or batch file.` or an incorrect version, you'll need to configure your system paths.

System Paths:
TODO

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
In the project directory run the follow.
```bash
mvnw clean install
```
There will be two jar files.
1. character-chat-app.jar : This is the executable file.
2. character-chat-app-1.0-SNAPSHOT.jar : Compiled classes only.

Place executable jar into `%APPDATA%/chat-app/`.
